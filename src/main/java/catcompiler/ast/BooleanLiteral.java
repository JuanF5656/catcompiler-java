package catcompiler.ast;

import catcompiler.token.Token;

/**
 * true / false.
 * (En Go se llama Boolean; aquí BooleanLiteral para no chocar con java.lang.Boolean.)
 */
public class BooleanLiteral implements Expression {
    public final Token token;
    public final boolean value;

    public BooleanLiteral(Token token, boolean value) {
        this.token = token;
        this.value = value;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return token.literal();
    }
}
