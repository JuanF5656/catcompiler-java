package catcompiler.token;

import java.util.Map;

/**
 * Token: unidad mínima que produce el Lexer (tipo + texto literal).
 */
public record Token(TokenType type, String literal) {

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

    /** Mismo formato que fmt.Printf("%+v") en Go: {Type:LET Literal:let} */
    @Override
    public String toString() {
        return "{Type:" + type + " Literal:" + literal + "}";
    }
}
