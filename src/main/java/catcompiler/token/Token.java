package catcompiler.token;

import java.util.Map;

/**
 * Token: unidad mínima que produce el Lexer (tipo + texto literal + posición).
 * La línea y la columna viajan con el token para que el parser y el evaluador
 * puedan señalar exactamente dónde está un error, sin volver a mirar el texto.
 */
public record Token(TokenType type, String literal, int line, int column) {

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("function", TokenType.FUNCTION),
            Map.entry("let", TokenType.LET),
            Map.entry("true", TokenType.TRUE),
            Map.entry("false", TokenType.FALSE),
            Map.entry("if", TokenType.IF),
            Map.entry("else", TokenType.ELSE),
            Map.entry("elif", TokenType.ELIF),
            Map.entry("return", TokenType.RETURN),
            Map.entry("for", TokenType.FOR),
            Map.entry("while", TokenType.WHILE),
            Map.entry("break", TokenType.BREAK));

    /**
     * Si el identificador es una palabra reservada devuelve su tipo; si no, IDENT.
     */
    public static TokenType lookupIdent(String ident) {
        return KEYWORDS.getOrDefault(ident, TokenType.IDENT);
    }

    /** Prefijo común de todos los mensajes de error: "[línea L, col C]". */
    public String position() {
        return "[línea " + line + ", col " + column + "]";
    }

    /** Mismo formato que la versión en Go, más la posición: {Type:LET Literal:let Pos:1:1} */
    @Override
    public String toString() {
        return "{Type:" + type + " Literal:" + literal + " Pos:" + line + ":" + column + "}";
    }
}
