package catcompiler.ast;

import catcompiler.token.Token;

/**
 * Número entero escrito en el código (ej. 42). Guarda el valor ya convertido a long.
 */
public class IntegerLiteral implements Expression {
    public final Token token;
    public long value;

    public IntegerLiteral(Token token) {
        this.token = token;
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
