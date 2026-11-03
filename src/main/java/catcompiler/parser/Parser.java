package catcompiler.parser;

import catcompiler.ast.*;
import catcompiler.lexer.Lexer;
import catcompiler.token.Token;
import catcompiler.token.TokenType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Parser (analizador sintáctico): toma los tokens del Lexer y construye el AST.
 * Usa "Pratt Parsing" (Top-Down Operator Precedence).
 *
 * Ejemplo: "1 + 2 * 3" -> (1 + (2 * 3))
 */
public class Parser {

    // ======================= TABLA DE PRECEDENCIAS =======================
    public static final int LOWEST = 1;      // el piso
    public static final int EQUALS = 2;      // == !=
    public static final int LESSGREATER = 3; // < > <= >=
    public static final int SUM = 4;         // + -
    public static final int PRODUCT = 5;     // * /
    public static final int PREFIX = 6;      // -X !X
    public static final int EXPONENT = 7;    // **
    public static final int CALL = 8;        // miFuncion(X)

    private static final Map<TokenType, Integer> PRECEDENCES = new EnumMap<>(TokenType.class);

    static {
        PRECEDENCES.put(TokenType.EQ, EQUALS);
        PRECEDENCES.put(TokenType.NOT_EQ, EQUALS);
        PRECEDENCES.put(TokenType.LT, LESSGREATER);
        PRECEDENCES.put(TokenType.GT, LESSGREATER);
        PRECEDENCES.put(TokenType.LTE, LESSGREATER);
        PRECEDENCES.put(TokenType.GTE, LESSGREATER);
        PRECEDENCES.put(TokenType.PLUS, SUM);
        PRECEDENCES.put(TokenType.MINUS, SUM);
        PRECEDENCES.put(TokenType.SLASH, PRODUCT);
        PRECEDENCES.put(TokenType.ASTERISK, PRODUCT);
        PRECEDENCES.put(TokenType.POW, EXPONENT);
        PRECEDENCES.put(TokenType.LPAREN, CALL);
    }

    // ============================== ESTADO ==============================
    private final Lexer lexer;
    private final List<String> errors = new ArrayList<>();

    private Token curToken;   // token actual
    private Token peekToken;  // siguiente token (para "espiar")

    // Diccionarios de Pratt: tipo de token -> función que sabe parsearlo
    //  prefix: el token aparece AL INICIO de una expresión (5, x, -5, !true, (...))
    //  infix:  el token aparece EN MEDIO (5 + 3); recibe la expresión izquierda
    private final Map<TokenType, Supplier<Expression>> prefixParseFns = new EnumMap<>(TokenType.class);
    private final Map<TokenType, Function<Expression, Expression>> infixParseFns = new EnumMap<>(TokenType.class);

    public Parser(Lexer lexer) {
        this.lexer = lexer;

        // Registro de funciones PREFIX
        prefixParseFns.put(TokenType.IDENT, this::parseIdentifier);
        prefixParseFns.put(TokenType.INT, this::parseIntegerLiteral);
        prefixParseFns.put(TokenType.BANG, this::parsePrefixExpression);
        prefixParseFns.put(TokenType.MINUS, this::parsePrefixExpression);
        prefixParseFns.put(TokenType.TRUE, this::parseBoolean);
        prefixParseFns.put(TokenType.FALSE, this::parseBoolean);
        prefixParseFns.put(TokenType.LPAREN, this::parseGroupedExpression);
        prefixParseFns.put(TokenType.IF, this::parseIfExpression);
        prefixParseFns.put(TokenType.FUNCTION, this::parseFunctionLiteral);
        prefixParseFns.put(TokenType.WHILE, this::parseWhileExpression);
        prefixParseFns.put(TokenType.FOR, this::parseForExpression);

        // Registro de funciones INFIX
        for (TokenType t : List.of(TokenType.PLUS, TokenType.MINUS, TokenType.SLASH, TokenType.ASTERISK,
                TokenType.EQ, TokenType.NOT_EQ, TokenType.LT, TokenType.GT,
                TokenType.LTE, TokenType.GTE, TokenType.POW)) {
            infixParseFns.put(t, this::parseInfixExpression);
        }
        infixParseFns.put(TokenType.LPAREN, this::parseCallExpression);

        // Leemos dos tokens para llenar curToken y peekToken
        nextToken();
        nextToken();
    }

    public List<String> errors() {
        return errors;
    }

    private void nextToken() {
        curToken = peekToken;
        peekToken = lexer.nextToken();
    }

    // ========================== PUNTO DE ENTRADA ==========================

    /** Lee todos los tokens hasta EOF y construye el nodo raíz Program. */
    public Program parseProgram() {
        Program program = new Program();
        while (curToken.type() != TokenType.EOF) {
            Statement stmt = parseStatement();
            if (stmt != null) {
                program.statements.add(stmt);
            }
            nextToken();
        }
        return program;
    }

    private Statement parseStatement() {
        switch (curToken.type()) {
            case LET:
                return parseLetStatement();
            case RETURN:
                return parseReturnStatement();
            default:
                return parseExpressionStatement();
        }
    }

    // ============================= SENTENCIAS =============================

    /** LET <IDENT> = <EXPRESIÓN> ; */
    private LetStatement parseLetStatement() {
        LetStatement stmt = new LetStatement(curToken);

        if (!expectPeek(TokenType.IDENT)) {
            return null;
        }
        stmt.name = new Identifier(curToken, curToken.literal());

        if (!expectPeek(TokenType.ASSIGN)) {
            return null;
        }

        nextToken();
        stmt.value = parseExpression(LOWEST);

        if (peekTokenIs(TokenType.SEMICOLON)) {
            nextToken();
        }
        return stmt;
    }

    /** RETURN <EXPRESIÓN> ; */
    private ReturnStatement parseReturnStatement() {
        ReturnStatement stmt = new ReturnStatement(curToken);
        nextToken();
        stmt.returnValue = parseExpression(LOWEST);
        if (peekTokenIs(TokenType.SEMICOLON)) {
            nextToken();
        }
        return stmt;
    }

    /** Una expresión suelta como `5 + 3;` */
    private ExpressionStatement parseExpressionStatement() {
        ExpressionStatement stmt = new ExpressionStatement(curToken);
        stmt.expression = parseExpression(LOWEST);
        if (peekTokenIs(TokenType.SEMICOLON)) {
            nextToken();
        }
        return stmt;
    }

    /** Todo lo que está dentro de { ... } */
    private BlockStatement parseBlockStatement() {
        BlockStatement block = new BlockStatement(curToken);
        nextToken(); // saltamos "{"

        while (!curTokenIs(TokenType.RBRACE) && !curTokenIs(TokenType.EOF)) {
            Statement stmt = parseStatement();
            if (stmt != null) {
                block.statements.add(stmt);
            }
            nextToken();
        }
        return block;
    }

    // ==================== ★ PRATT PARSER: EL NÚCLEO ★ ====================
    //
    // REGLA DE ORO: sigue agrupando hacia la derecha mientras el siguiente
    // token tenga MAYOR precedencia que la precedencia actual.

    private Expression parseExpression(int precedence) {
        // 1. Buscar la función PREFIX del token actual
        Supplier<Expression> prefix = prefixParseFns.get(curToken.type());
        if (prefix == null) {
            noPrefixParseFnError(curToken.type());
            return null;
        }

        // 2. Ejecutarla para obtener la expresión izquierda
        Expression leftExp = prefix.get();

        // 3. El loop de Pratt
        while (!peekTokenIs(TokenType.SEMICOLON) && precedence < peekPrecedence()) {
            Function<Expression, Expression> infix = infixParseFns.get(peekToken.type());
            if (infix == null) {
                return leftExp;
            }
            nextToken();               // avanzamos al operador
            leftExp = infix.apply(leftExp);
        }
        return leftExp;
    }

    // ========================== FUNCIONES PREFIX ==========================

    private Expression parseIdentifier() {
        return new Identifier(curToken, curToken.literal());
    }

    private Expression parseIntegerLiteral() {
        IntegerLiteral lit = new IntegerLiteral(curToken);
        try {
            lit.value = Long.parseLong(curToken.literal());
        } catch (NumberFormatException e) {
            errors.add("No se pudo parsear \"" + curToken.literal() + "\" como entero");
            return null;
        }
        return lit;
    }

    /** -5  !true */
    private Expression parsePrefixExpression() {
        PrefixExpression expression = new PrefixExpression(curToken, curToken.literal());
        nextToken();
        expression.right = parseExpression(PREFIX);
        return expression;
    }

    private Expression parseBoolean() {
        return new BooleanLiteral(curToken, curTokenIs(TokenType.TRUE));
    }

    /** (5 + 3): los paréntesis no crean nodo, solo cambian la precedencia */
    private Expression parseGroupedExpression() {
        nextToken(); // saltamos "("
        Expression exp = parseExpression(LOWEST);
        if (!expectPeek(TokenType.RPAREN)) {
            return null;
        }
        return exp;
    }

    /** if (<condición>) { ... } else { ... } */
    private Expression parseIfExpression() {
        IfExpression expression = new IfExpression(curToken);

        if (!expectPeek(TokenType.LPAREN)) return null;
        nextToken();
        expression.condition = parseExpression(LOWEST);
        if (!expectPeek(TokenType.RPAREN)) return null;
        if (!expectPeek(TokenType.LBRACE)) return null;

        expression.consequence = parseBlockStatement();

        if (peekTokenIs(TokenType.ELSE)) {
            nextToken();
            if (!expectPeek(TokenType.LBRACE)) return null;
            expression.alternative = parseBlockStatement();
        }
        return expression;
    }

    /** while (<condición>) { ... } */
    private Expression parseWhileExpression() {
        WhileExpression expression = new WhileExpression(curToken);

        if (!expectPeek(TokenType.LPAREN)) return null;
        nextToken();
        expression.condition = parseExpression(LOWEST);
        if (!expectPeek(TokenType.RPAREN)) return null;
        if (!expectPeek(TokenType.LBRACE)) return null;

        expression.body = parseBlockStatement();
        return expression;
    }

    /** for (<condición>) { ... } */
    private Expression parseForExpression() {
        ForExpression expression = new ForExpression(curToken);

        if (!expectPeek(TokenType.LPAREN)) return null;
        nextToken();
        expression.condition = parseExpression(LOWEST);
        if (!expectPeek(TokenType.RPAREN)) return null;
        if (!expectPeek(TokenType.LBRACE)) return null;

        expression.body = parseBlockStatement();
        return expression;
    }

    /** function(<params>) { ... } */
    private Expression parseFunctionLiteral() {
        FunctionLiteral lit = new FunctionLiteral(curToken);

        if (!expectPeek(TokenType.LPAREN)) return null;
        lit.parameters = parseFunctionParameters();
        if (!expectPeek(TokenType.LBRACE)) return null;

        lit.body = parseBlockStatement();
        return lit;
    }

    private List<Identifier> parseFunctionParameters() {
        List<Identifier> identifiers = new ArrayList<>();

        if (peekTokenIs(TokenType.RPAREN)) { // function() sin parámetros
            nextToken();
            return identifiers;
        }

        nextToken();
        identifiers.add(new Identifier(curToken, curToken.literal()));

        while (peekTokenIs(TokenType.COMMA)) {
            nextToken(); // saltamos la coma
            nextToken(); // siguiente parámetro
            identifiers.add(new Identifier(curToken, curToken.literal()));
        }

        if (!expectPeek(TokenType.RPAREN)) {
            return new ArrayList<>();
        }
        return identifiers;
    }

    // ========================== FUNCIONES INFIX ==========================

    /** 5 + 3, x == y, 2 ** 3 ... (recibe lo que ya se parseó a la izquierda) */
    private Expression parseInfixExpression(Expression left) {
        InfixExpression expression = new InfixExpression(curToken, curToken.literal(), left);
        int precedence = curPrecedence();
        nextToken();
        expression.right = parseExpression(precedence);
        return expression;
    }

    /** add(2, 3): se activa al ver "(" después de una expresión */
    private Expression parseCallExpression(Expression function) {
        CallExpression exp = new CallExpression(curToken, function);
        exp.arguments = parseCallArguments();
        return exp;
    }

    private List<Expression> parseCallArguments() {
        List<Expression> args = new ArrayList<>();

        if (peekTokenIs(TokenType.RPAREN)) { // add() sin argumentos
            nextToken();
            return args;
        }

        nextToken();
        args.add(parseExpression(LOWEST));

        while (peekTokenIs(TokenType.COMMA)) {
            nextToken();
            nextToken();
            args.add(parseExpression(LOWEST));
        }

        if (!expectPeek(TokenType.RPAREN)) {
            return new ArrayList<>();
        }
        return args;
    }

    // ============================== HELPERS ==============================

    private boolean curTokenIs(TokenType t) {
        return curToken.type() == t;
    }

    private boolean peekTokenIs(TokenType t) {
        return peekToken.type() == t;
    }

    /** "Guardián de la gramática": si el siguiente token es el esperado avanza; si no, registra error. */
    private boolean expectPeek(TokenType t) {
        if (peekTokenIs(t)) {
            nextToken();
            return true;
        }
        peekError(t);
        return false;
    }

    private void peekError(TokenType t) {
        errors.add("Error sintáctico: Se esperaba el token " + t + ", se obtuvo " + peekToken.type());
    }

    private void noPrefixParseFnError(TokenType t) {
        errors.add("No se encontró función de parseo (prefix) para el token " + t);
    }

    private int peekPrecedence() {
        return PRECEDENCES.getOrDefault(peekToken.type(), LOWEST);
    }

    private int curPrecedence() {
        return PRECEDENCES.getOrDefault(curToken.type(), LOWEST);
    }
}
