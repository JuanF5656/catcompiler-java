package catcompiler.ast;

import catcompiler.token.Token;

/**
 * String escrito en el código: "hola". El lexer ya resolvió los escapes,
 * así que value es el texto final.
 */
public class StringLiteral implements Expression {
    public final Token token;
    public final String value;

    public StringLiteral(Token token) {
        this.token = token;
        this.value = token.literal();
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
