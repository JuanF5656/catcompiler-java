package catcompiler.token;

/**
 * Tipos de token. Cada tipo guarda el mismo "valor" que usa la versión en Go
 * (por ejemplo ASSIGN -> "=", LET -> "LET"), así la salida del REPL es idéntica.
 */
public enum TokenType {
    ILLEGAL("ILLEGAL"),
    EOF("EOF"),

    // Identificadores + literales
    IDENT("IDENT"), // add, foobar, x, y, ...
    INT("INT"),     // 1343456

    // Operadores
    ASSIGN("="),
    PLUS("+"),
    MINUS("-"),
    BANG("!"),
    ASTERISK("*"),
    SLASH("/"),

    LT("<"),
    GT(">"),
    LTE("<="),
    GTE(">="),

    EQ("=="),
    NOT_EQ("!="),

    POW("**"),

    // Delimitadores
    COMMA(","),
    SEMICOLON(";"),

    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),
    LBRACKET("["),
    RBRACKET("]"),

    // Palabras reservadas
    FUNCTION("FUNCTION"),
    LET("LET"),
    TRUE("TRUE"),
    FALSE("FALSE"),
    IF("IF"),
    ELSE("ELSE"),
    ELIF("ELIF"),
    RETURN("RETURN"),
    FOR("FOR"),
    WHILE("WHILE"),
    BREAK("BREAK");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
