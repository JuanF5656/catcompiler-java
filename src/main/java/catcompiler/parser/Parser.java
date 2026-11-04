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
 *
 * Política de errores: solo se guarda el PRIMER error y se deja de parsear.
 * Después de un error el parser queda desalineado y todo lo que reporte
 * serían errores "en cascada" que confunden más de lo que ayudan.
 */
public class Parser {

    // ======================= TABLA DE PRECEDENCIAS =======================
    // De menor a mayor. Un operador "atrapa" a sus operandos antes que los de
    // menor precedencia: en a || b && c, el && se agrupa primero.
    public static final int LOWEST = 1;      // el piso
    public static final int ASSIGN = 2;      // x = valor   (derecha-asociativo)
    public static final int OR = 3;          // ||
    public static final int AND = 4;         // &&
    public static final int EQUALS = 5;      // == !=
    public static final int LESSGREATER = 6; // < > <= >=
    public static final int SUM = 7;         // + -
    public static final int PRODUCT = 8;     // * / %
    public static final int EXPONENT = 9;    // **          (derecha-asociativo)
    public static final int PREFIX = 10;     // -X !X
    public static final int CALL = 11;       // miFuncion(X)  lista[i]

    private static final Map<TokenType, Integer> PRECEDENCES = new EnumMap<>(TokenType.class);

    static {
        PRECEDENCES.put(TokenType.ASSIGN, ASSIGN);
        PRECEDENCES.put(TokenType.OR, OR);
        PRECEDENCES.put(TokenType.AND, AND);
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
        PRECEDENCES.put(TokenType.PERCENT, PRODUCT);
        PRECEDENCES.put(TokenType.POW, EXPONENT);
        PRECEDENCES.put(TokenType.LPAREN, CALL);
        PRECEDENCES.put(TokenType.LBRACKET, CALL);
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
        prefixParseFns.put(TokenType.STRING, this::parseStringLiteral);
        prefixParseFns.put(TokenType.BANG, this::parsePrefixExpression);
        prefixParseFns.put(TokenType.MINUS, this::parsePrefixExpression);
        prefixParseFns.put(TokenType.TRUE, this::parseBoolean);
        prefixParseFns.put(TokenType.FALSE, this::parseBoolean);
        prefixParseFns.put(TokenType.LPAREN, this::parseGroupedExpression);
        prefixParseFns.put(TokenType.LBRACKET, this::parseArrayLiteral);
        prefixParseFns.put(TokenType.LBRACE, this::parseHashLiteral);
        prefixParseFns.put(TokenType.IF, this::parseIfExpression);
        prefixParseFns.put(TokenType.FUNCTION, this::parseFunctionLiteral);
        prefixParseFns.put(TokenType.WHILE, this::parseWhileExpression);
        prefixParseFns.put(TokenType.FOR, this::parseForExpression);

        // Registro de funciones INFIX
        for (TokenType t : List.of(TokenType.PLUS, TokenType.MINUS, TokenType.SLASH, TokenType.ASTERISK,
                TokenType.PERCENT, TokenType.EQ, TokenType.NOT_EQ, TokenType.LT, TokenType.GT,
                TokenType.LTE, TokenType.GTE, TokenType.POW, TokenType.AND, TokenType.OR)) {
            infixParseFns.put(t, this::parseInfixExpression);
        }
        infixParseFns.put(TokenType.ASSIGN, this::parseAssignExpression);
        infixParseFns.put(TokenType.LPAREN, this::parseCallExpression);
        infixParseFns.put(TokenType.LBRACKET, this::parseIndexExpression);

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

    /** Lee todos los tokens hasta EOF (o hasta el primer error) y construye el nodo raíz Program. */
    public Program parseProgram() {
        Program program = new Program();
        while (!curTokenIs(TokenType.EOF) && errors.isEmpty()) {
            Statement stmt = parseStatement();
            if (stmt != null) {
                program.statements.add(stmt);
            }
            nextToken();
        }
        return program;
    }

    private Statement parseStatement() {
        return switch (curToken.type()) {
            case LET -> parseLetStatement();
            case RETURN -> parseReturnStatement();
            case BREAK -> parseBreakStatement();
            default -> parseExpressionStatement();
        };
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
        skipOptionalSemicolon();
        return stmt;
    }

    /** RETURN [<EXPRESIÓN>] ;   (sin expresión retorna null) */
    private ReturnStatement parseReturnStatement() {
        ReturnStatement stmt = new ReturnStatement(curToken);
        if (!peekTokenIs(TokenType.SEMICOLON) && !peekTokenIs(TokenType.RBRACE)) {
            nextToken();
            stmt.returnValue = parseExpression(LOWEST);
        }
        skipOptionalSemicolon();
        return stmt;
    }

    /** BREAK ; */
    private BreakStatement parseBreakStatement() {
        BreakStatement stmt = new BreakStatement(curToken);
        skipOptionalSemicolon();
        return stmt;
    }

    /** Una expresión suelta como `5 + 3;` */
    private ExpressionStatement parseExpressionStatement() {
        ExpressionStatement stmt = new ExpressionStatement(curToken);
        stmt.expression = parseExpression(LOWEST);
        skipOptionalSemicolon();
        return stmt;
    }

    /** El ';' es opcional al final de una sentencia. */
    private void skipOptionalSemicolon() {
        if (peekTokenIs(TokenType.SEMICOLON)) {
            nextToken();
        }
    }

    /** Todo lo que está dentro de { ... } */
    private BlockStatement parseBlockStatement() {
        BlockStatement block = new BlockStatement(curToken);
        nextToken(); // saltamos "{"

        while (!curTokenIs(TokenType.RBRACE) && !curTokenIs(TokenType.EOF) && errors.isEmpty()) {
            Statement stmt = parseStatement();
            if (stmt != null) {
                block.statements.add(stmt);
            }
            nextToken();
        }
        if (curTokenIs(TokenType.EOF)) {
            expectedError(TokenType.RBRACE, curToken);
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
            syntaxError(curToken, "expresión inválida, no se esperaba " + describe(curToken));
            return null;
        }

        // 2. Ejecutarla para obtener la expresión izquierda
        Expression leftExp = prefix.get();

        // 3. El loop de Pratt
        while (!peekTokenIs(TokenType.SEMICOLON) && precedence < peekPrecedence()
                && !closesBlockAtEndOfLine() && errors.isEmpty()) {
            Function<Expression, Expression> infix = infixParseFns.get(peekToken.type());
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
            syntaxError(curToken, "el entero " + curToken.literal() + " es demasiado grande");
            return null;
        }
        return lit;
    }

    private Expression parseStringLiteral() {
        return new StringLiteral(curToken);
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

    /** [1, 2, 3] */
    private Expression parseArrayLiteral() {
        ArrayLiteral array = new ArrayLiteral(curToken);
        array.elements = parseExpressionList(TokenType.RBRACKET);
        return array.elements == null ? null : array;
    }

    /**
     * {clave: valor, ...}. Un '{' al inicio de una expresión siempre es un
     * diccionario: los bloques solo aparecen después de if/while/for/function,
     * y esos los parsea parseBlockStatement directamente, sin pasar por aquí.
     */
    private Expression parseHashLiteral() {
        HashLiteral hash = new HashLiteral(curToken);
        while (!peekTokenIs(TokenType.RBRACE)) {
            nextToken();
            Expression key = parseExpression(LOWEST);
            if (!expectPeek(TokenType.COLON)) {
                return null;
            }
            nextToken();
            hash.pairs.add(new HashLiteral.Pair(key, parseExpression(LOWEST)));
            if (!peekTokenIs(TokenType.RBRACE) && !expectPeek(TokenType.COMMA)) {
                return null;
            }
        }
        nextToken(); // saltamos "}"
        return hash;
    }

    /**
     * if (<cond>) { ... } elif (<cond>) { ... } else { ... }
     * Un elif se guarda como un else cuyo bloque contiene otro if: así el
     * evaluador no necesita saber que elif existe.
     */
    private Expression parseIfExpression() {
        IfExpression expression = new IfExpression(curToken);
        expression.condition = parseCondition();
        if (expression.condition == null) return null;
        expression.consequence = parseBody();
        if (expression.consequence == null) return null;

        if (peekTokenIs(TokenType.ELIF)) {
            nextToken();
            expression.alternative = wrapInBlock(parseIfExpression());
            if (expression.alternative == null) return null;
        } else if (peekTokenIs(TokenType.ELSE)) {
            nextToken();
            expression.alternative = parseBody();
            if (expression.alternative == null) return null;
        }
        return expression;
    }

    private BlockStatement wrapInBlock(Expression expression) {
        if (expression == null) {
            return null;
        }
        ExpressionStatement stmt = new ExpressionStatement(curToken);
        stmt.expression = expression;
        BlockStatement block = new BlockStatement(curToken);
        block.statements.add(stmt);
        return block;
    }

    /** while (<condición>) { ... } */
    private Expression parseWhileExpression() {
        WhileExpression expression = new WhileExpression(curToken);
        expression.condition = parseCondition();
        if (expression.condition == null) return null;
        expression.body = parseBody();
        return expression.body == null ? null : expression;
    }

    /**
     * for (<condición>) { ... }   o   for (<init>; <condición>; <update>) { ... }
     * Se distinguen por el ';' después de la primera parte.
     */
    private Expression parseForExpression() {
        ForExpression expression = new ForExpression(curToken);
        if (!expectPeek(TokenType.LPAREN)) return null;
        nextToken();

        Statement first = curTokenIs(TokenType.LET) ? parseLetStatement() : parseExpressionStatement();
        if (first == null) return null;

        if (curTokenIs(TokenType.SEMICOLON)) {
            expression.init = first;
            nextToken();
            expression.condition = parseExpression(LOWEST);
            if (!expectPeek(TokenType.SEMICOLON)) return null;
            nextToken();
            expression.update = parseExpression(LOWEST);
        } else if (first instanceof ExpressionStatement stmt) {
            expression.condition = stmt.expression;
        } else {
            expectedError(TokenType.SEMICOLON, peekToken); // "for (let i = 0)" sin el resto
            return null;
        }

        if (!expectPeek(TokenType.RPAREN)) return null;
        expression.body = parseBody();
        return expression.body == null ? null : expression;
    }

    /** function [nombre](<params>) { ... } */
    private Expression parseFunctionLiteral() {
        FunctionLiteral lit = new FunctionLiteral(curToken);

        if (peekTokenIs(TokenType.IDENT)) {
            nextToken();
            lit.name = new Identifier(curToken, curToken.literal());
        }
        if (!expectPeek(TokenType.LPAREN)) return null;
        lit.parameters = parseFunctionParameters();
        if (lit.parameters == null) return null;

        lit.body = parseBody();
        return lit.body == null ? null : lit;
    }

    private List<Identifier> parseFunctionParameters() {
        List<Identifier> identifiers = new ArrayList<>();

        if (peekTokenIs(TokenType.RPAREN)) { // function() sin parámetros
            nextToken();
            return identifiers;
        }

        do {
            if (!identifiers.isEmpty()) {
                nextToken(); // saltamos la coma
            }
            if (!expectPeek(TokenType.IDENT)) return null;
            identifiers.add(new Identifier(curToken, curToken.literal()));
        } while (peekTokenIs(TokenType.COMMA));

        return expectPeek(TokenType.RPAREN) ? identifiers : null;
    }

    // ========================== FUNCIONES INFIX ==========================

    /** 5 + 3, x == y, a && b, 2 ** 3 ... (recibe lo que ya se parseó a la izquierda) */
    private Expression parseInfixExpression(Expression left) {
        InfixExpression expression = new InfixExpression(curToken, curToken.literal(), left);
        int precedence = curPrecedence();
        // Truco de Pratt para asociar a la derecha: el lado derecho se parsea con
        // precedencia un poco MENOR, así otro ** a su derecha sí lo "atrapa".
        // 2 ** 3 ** 2 -> (2 ** (3 ** 2))
        if (curTokenIs(TokenType.POW)) {
            precedence--;
        }
        nextToken();
        expression.right = parseExpression(precedence);
        return expression;
    }

    /**
     * x = valor  /  lista[i] = valor. También derecha-asociativo (a = b = 1),
     * por eso el valor se parsea con LOWEST (= ASSIGN - 1).
     */
    private Expression parseAssignExpression(Expression target) {
        if (!(target instanceof Identifier) && !(target instanceof IndexExpression)) {
            syntaxError(curToken, "no se puede asignar a " + target + "; solo a una variable o a lista[i]");
            return null;
        }
        AssignExpression expression = new AssignExpression(curToken, target);
        nextToken();
        expression.value = parseExpression(LOWEST);
        return expression;
    }

    /** add(2, 3): se activa al ver "(" después de una expresión */
    private Expression parseCallExpression(Expression function) {
        CallExpression exp = new CallExpression(curToken, function);
        exp.arguments = parseExpressionList(TokenType.RPAREN);
        return exp.arguments == null ? null : exp;
    }

    /** lista[0]: se activa al ver "[" después de una expresión */
    private Expression parseIndexExpression(Expression left) {
        IndexExpression exp = new IndexExpression(curToken, left);
        nextToken();
        exp.index = parseExpression(LOWEST);
        return expectPeek(TokenType.RBRACKET) ? exp : null;
    }

    /** Lista de expresiones separadas por coma hasta `end`: argumentos o elementos de lista. */
    private List<Expression> parseExpressionList(TokenType end) {
        List<Expression> list = new ArrayList<>();

        if (peekTokenIs(end)) { // f() o [] vacíos
            nextToken();
            return list;
        }

        nextToken();
        list.add(parseExpression(LOWEST));

        while (peekTokenIs(TokenType.COMMA)) {
            nextToken();
            nextToken();
            list.add(parseExpression(LOWEST));
        }

        return expectPeek(end) ? list : null;
    }

    // ============================== HELPERS ==============================

    /** ( <expresión> ) de if y while. */
    private Expression parseCondition() {
        if (!expectPeek(TokenType.LPAREN)) return null;
        nextToken();
        Expression condition = parseExpression(LOWEST);
        return expectPeek(TokenType.RPAREN) ? condition : null;
    }

    /** { ... } obligatorio después de if/elif/else/while/for/function. */
    private BlockStatement parseBody() {
        return expectPeek(TokenType.LBRACE) ? parseBlockStatement() : null;
    }

    /**
     * Como el ';' es opcional, sin esta regla "if (c) { 1 }" seguido de una
     * línea que empieza con "-1" o "(x)" se leería como una resta o una
     * llamada. Una expresión que termina en "}" al final de la línea se corta ahí.
     */
    private boolean closesBlockAtEndOfLine() {
        return curTokenIs(TokenType.RBRACE) && peekToken.line() > curToken.line();
    }

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
        expectedError(t, peekToken);
        return false;
    }

    private void expectedError(TokenType expected, Token found) {
        syntaxError(found, "se esperaba " + describe(expected) + " pero se encontró " + describe(found));
    }

    /**
     * Registra un error sintáctico (solo el primero, ver comentario de la clase).
     * Si el lexer ya encontró un error léxico, ese se reporta en su lugar: el
     * error sintáctico casi siempre es consecuencia del token ILLEGAL.
     */
    private void syntaxError(Token at, String detail) {
        if (!errors.isEmpty()) {
            return;
        }
        if (!lexer.errors().isEmpty()) {
            errors.add(lexer.errors().get(0));
        } else {
            errors.add("Error sintáctico " + at.position() + ": " + detail);
        }
    }

    /** Nombre legible de un tipo de token para los mensajes ("')'", "un identificador"). */
    private static String describe(TokenType t) {
        return switch (t) {
            case IDENT -> "un identificador";
            case INT -> "un entero";
            case STRING -> "un string";
            case EOF -> "el fin del archivo";
            default -> "'" + t.getValue().toLowerCase() + "'";
        };
    }

    /** Cómo se muestra el token que realmente apareció. */
    private static String describe(Token t) {
        return switch (t.type()) {
            case EOF -> "el fin del archivo";
            case STRING -> "\"" + t.literal() + "\"";
            default -> "'" + t.literal() + "'";
        };
    }

    private int peekPrecedence() {
        return PRECEDENCES.getOrDefault(peekToken.type(), LOWEST);
    }

    private int curPrecedence() {
        return PRECEDENCES.getOrDefault(curToken.type(), LOWEST);
    }
}
