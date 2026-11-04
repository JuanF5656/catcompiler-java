package catcompiler.lexer;

import catcompiler.token.Token;
import catcompiler.token.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Lexer: analizador léxico. Convierte el texto fuente en tokens bajo demanda
 * (el parser pide uno a la vez con nextToken()).
 *
 * Los errores léxicos no detienen el lexer: se emite un token ILLEGAL y el
 * mensaje queda en errors(). Así el parser decide cuándo parar y el modo
 * Lexer del REPL puede seguir mostrando el resto de tokens.
 */
public class Lexer {
    private final String input;
    private int position;      // posición actual en input (apunta al carácter actual)
    private int readPosition;  // posición de lectura (después del carácter actual)
    private char ch;           // carácter actual bajo análisis

    // Posición (1-based) del carácter actual, para los mensajes de error
    private int line = 1;
    private int column = 0;

    // Posición donde empezó el token que se está leyendo
    private int startLine;
    private int startColumn;

    private final List<String> errors = new ArrayList<>();

    public Lexer(String input) {
        this.input = input;
        readChar();
    }

    public List<String> errors() {
        return errors;
    }

    /**
     * Avanza un carácter. La línea se incrementa al SALIR de un '\n' (no al
     * llegar), para que el propio salto de línea siga perteneciendo a su línea.
     */
    private void readChar() {
        if (ch == '\n') {
            line++;
            column = 0;
        }
        ch = readPosition >= input.length() ? 0 : input.charAt(readPosition);
        position = readPosition;
        readPosition++;
        column++;
    }

    /** Evalúa el carácter actual y devuelve el token correspondiente. */
    public Token nextToken() {
        skipWhitespaceAndComments();
        startLine = line;
        startColumn = column;

        if (isLetter(ch)) {
            String literal = readWhile(Lexer::isIdentifierChar);
            return token(Token.lookupIdent(literal), literal);
        }
        if (isDigit(ch)) {
            return token(TokenType.INT, readWhile(Lexer::isDigit));
        }
        if (ch == '"') {
            return readString();
        }

        Token tok = switch (ch) {
            case '=' -> oneOrTwo('=', TokenType.ASSIGN, TokenType.EQ);
            case '!' -> oneOrTwo('=', TokenType.BANG, TokenType.NOT_EQ);
            case '<' -> oneOrTwo('=', TokenType.LT, TokenType.LTE);
            case '>' -> oneOrTwo('=', TokenType.GT, TokenType.GTE);
            case '*' -> oneOrTwo('*', TokenType.ASTERISK, TokenType.POW);
            case '&' -> doubleOnly('&', TokenType.AND);
            case '|' -> doubleOnly('|', TokenType.OR);
            case '+' -> single(TokenType.PLUS);
            case '-' -> single(TokenType.MINUS);
            case '/' -> single(TokenType.SLASH);
            case '%' -> single(TokenType.PERCENT);
            case ';' -> single(TokenType.SEMICOLON);
            case ':' -> single(TokenType.COLON);
            case ',' -> single(TokenType.COMMA);
            case '{' -> single(TokenType.LBRACE);
            case '}' -> single(TokenType.RBRACE);
            case '(' -> single(TokenType.LPAREN);
            case ')' -> single(TokenType.RPAREN);
            case '[' -> single(TokenType.LBRACKET);
            case ']' -> single(TokenType.RBRACKET);
            case 0 -> token(TokenType.EOF, "");
            default -> illegal("carácter ilegal '" + ch + "'", String.valueOf(ch));
        };
        readChar();
        return tok;
    }

    // ============================ OPERADORES ============================

    private Token single(TokenType type) {
        return token(type, String.valueOf(ch));
    }

    /** Operadores que pueden ser de uno o dos caracteres (=  ==, <  <=, *  **...). */
    private Token oneOrTwo(char second, TokenType oneChar, TokenType twoChars) {
        if (peekChar() != second) {
            return single(oneChar);
        }
        char first = ch;
        readChar();
        return token(twoChars, "" + first + ch);
    }

    /** && y ||: un '&' o '|' suelto no existe en el lenguaje, es un error léxico. */
    private Token doubleOnly(char c, TokenType type) {
        if (peekChar() != c) {
            return illegal("carácter ilegal '" + c + "' (¿quisiste decir '" + c + c + "'?)", String.valueOf(c));
        }
        readChar();
        return token(type, "" + c + c);
    }

    // ============================== STRINGS ==============================

    /**
     * Lee "..." procesando los escapes. Un string no puede cruzar líneas: si
     * aparece un salto de línea o el fin del archivo antes de la comilla de
     * cierre, se reporta como "string sin cerrar" en la posición de apertura.
     */
    private Token readString() {
        StringBuilder value = new StringBuilder();
        readChar(); // saltamos la comilla de apertura
        while (ch != '"') {
            if (ch == 0 || ch == '\n') {
                return illegal("string sin cerrar", "\"" + value);
            }
            if (ch == '\\') {
                readChar();
                char escaped = escapeOf(ch);
                if (escaped == 0) {
                    String bad = "\\" + (ch == 0 ? "" : ch);
                    readChar();
                    return illegal("secuencia de escape inválida '" + bad + "' en string", bad);
                }
                value.append(escaped);
            } else {
                value.append(ch);
            }
            readChar();
        }
        readChar(); // saltamos la comilla de cierre
        return token(TokenType.STRING, value.toString());
    }

    /** Traduce el carácter que sigue a '\'. Devuelve 0 si el escape no existe. */
    private static char escapeOf(char c) {
        return switch (c) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case '"' -> '"';
            case '\\' -> '\\';
            default -> 0;
        };
    }

    // ============================== HELPERS ==============================

    /** Ignora espacios y comentarios de línea (// hasta el final de la línea). */
    private void skipWhitespaceAndComments() {
        while (true) {
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                readChar();
            } else if (ch == '/' && peekChar() == '/') {
                while (ch != '\n' && ch != 0) {
                    readChar();
                }
            } else {
                return;
            }
        }
    }

    private String readWhile(CharPredicate accepts) {
        int start = position;
        while (accepts.test(ch)) {
            readChar();
        }
        return input.substring(start, position);
    }

    @FunctionalInterface
    private interface CharPredicate {
        boolean test(char c);
    }

    /** Un identificador empieza por letra o '_' (por eso isLetter) ... */
    private static boolean isLetter(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_';
    }

    /** ... y después también admite dígitos (ej. fib2, x_1). */
    private static boolean isIdentifierChar(char c) {
        return isLetter(c) || isDigit(c);
    }

    private static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    /** "Espía" el siguiente carácter sin avanzar (para operadores de dos caracteres). */
    private char peekChar() {
        return readPosition >= input.length() ? 0 : input.charAt(readPosition);
    }

    private Token token(TokenType type, String literal) {
        return new Token(type, literal, startLine, startColumn);
    }

    private Token illegal(String detail, String literal) {
        Token tok = token(TokenType.ILLEGAL, literal);
        errors.add("Error léxico " + tok.position() + ": " + detail);
        return tok;
    }
}
