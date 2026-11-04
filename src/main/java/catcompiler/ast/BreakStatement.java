package catcompiler.ast;

import catcompiler.token.Token;

/**
 * break;  sale del ciclo (while/for) más cercano.
 */
public class BreakStatement implements Statement {
    public final Token token;

    public BreakStatement(Token token) {
        this.token = token;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return "break;";
    }
}
