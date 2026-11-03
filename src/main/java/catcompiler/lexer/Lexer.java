package catcompiler.lexer;

import catcompiler.token.Token;
import catcompiler.token.TokenType;

/**
 * Lexer: analizador léxico.
 * Mantiene el estado del código fuente que se está analizando.
 */
public class Lexer {
    private final String input;
    private int position;      // posición actual en input (apunta al carácter actual)
    private int readPosition;  // posición de lectura (después del carácter actual)
    private char ch;           // carácter actual bajo análisis

    /**
     * Inicializa un nuevo Lexer con el código fuente proporcionado.
     * Llama a readChar() por primera vez para inicializar los punteros.
     */
    public Lexer(String input) {
        this.input = input;
        readChar();
    }

    /**
     * Avanza los punteros (position y readPosition) y actualiza el carácter actual (ch).
     * Si llega al final de la entrada, asigna 0 (NUL) a ch.
     */
    private void readChar() {
        if (readPosition >= input.length()) {
            ch = 0;
        } else {
            ch = input.charAt(readPosition);
        }
        position = readPosition;
        readPosition += 1;
    }

    /**
     * Evalúa el carácter actual, crea el token correspondiente,
     * avanza los punteros y retorna el token resultante.
     */
    public Token nextToken() {
        Token tok;

        skipWhitespace();

        switch (ch) {
            case '=':
                if (peekChar() == '=') {
                    tok = twoCharToken(TokenType.EQ);
                } else {
                    tok = newToken(TokenType.ASSIGN, ch);
                }
                break;
            case '+':
                tok = newToken(TokenType.PLUS, ch);
                break;
            case '-':
                tok = newToken(TokenType.MINUS, ch);
                break;
            case '!':
                if (peekChar() == '=') {
                    tok = twoCharToken(TokenType.NOT_EQ);
                } else {
                    tok = newToken(TokenType.BANG, ch);
                }
                break;
            case '/':
                tok = newToken(TokenType.SLASH, ch);
                break;
            case '*':
                if (peekChar() == '*') {
                    tok = twoCharToken(TokenType.POW);
                } else {
                    tok = newToken(TokenType.ASTERISK, ch);
                }
                break;
            case '<':
                if (peekChar() == '=') {
                    tok = twoCharToken(TokenType.LTE);
                } else {
                    tok = newToken(TokenType.LT, ch);
                }
                break;
            case '>':
                if (peekChar() == '=') {
                    tok = twoCharToken(TokenType.GTE);
                } else {
                    tok = newToken(TokenType.GT, ch);
                }
                break;
            case ';':
                tok = newToken(TokenType.SEMICOLON, ch);
                break;
            case ',':
                tok = newToken(TokenType.COMMA, ch);
                break;
            case '{':
                tok = newToken(TokenType.LBRACE, ch);
                break;
            case '}':
                tok = newToken(TokenType.RBRACE, ch);
                break;
            case '(':
                tok = newToken(TokenType.LPAREN, ch);
                break;
            case ')':
                tok = newToken(TokenType.RPAREN, ch);
                break;
            case '[':
                tok = newToken(TokenType.LBRACKET, ch);
                break;
            case ']':
                tok = newToken(TokenType.RBRACKET, ch);
                break;
            case 0:
                tok = new Token(TokenType.EOF, "");
                break;
            default:
                if (isLetter(ch)) {
                    String literal = readIdentifier();
                    return new Token(Token.lookupIdent(literal), literal);
                } else if (isDigit(ch)) {
                    return new Token(TokenType.INT, readNumber());
                } else {
                    tok = newToken(TokenType.ILLEGAL, ch);
                }
        }

        readChar();
        return tok;
    }

    /**
     * Crea un token de dos caracteres (==, !=, <=, >=, **).
     * Consume el carácter actual y el siguiente.
     */
    private Token twoCharToken(TokenType type) {
        char first = ch;
        readChar();
        String literal = "" + first + ch;
        return new Token(type, literal);
    }

    /**
     * Ignora espacios en blanco, tabulaciones y saltos de línea
     * avanzando los punteros hasta encontrar un carácter válido.
     */
    private void skipWhitespace() {
        while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
            readChar();
        }
    }

    /**
     * Lee de manera secuencial los caracteres mientras sean letras.
     * Retorna la palabra completa.
     */
    private String readIdentifier() {
        int start = position;
        while (isLetter(ch)) {
            readChar();
        }
        return input.substring(start, position);
    }

    /**
     * Verifica si un carácter es una letra o un guion bajo
     * (permite identificadores como 'mi_variable').
     */
    private static boolean isLetter(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_';
    }

    /**
     * Lee de manera secuencial los caracteres mientras sean dígitos.
     */
    private String readNumber() {
        int start = position;
        while (isDigit(ch)) {
            readChar();
        }
        return input.substring(start, position);
    }

    /** Verifica si el carácter es un dígito entre 0 y 9. */
    private static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    /**
     * "Espía" el siguiente carácter sin avanzar los punteros.
     * Útil para operadores de dos caracteres como '==' o '!='.
     */
    private char peekChar() {
        if (readPosition >= input.length()) {
            return 0;
        }
        return input.charAt(readPosition);
    }

    /** Función auxiliar para crear un Token de un solo carácter. */
    private static Token newToken(TokenType type, char c) {
        return new Token(type, String.valueOf(c));
    }
}
