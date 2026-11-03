package catcompiler.ast;

import catcompiler.token.Token;

/**
 * for (<condición>) { <cuerpo> }  (por ahora igual que while)
 */
public class ForExpression implements Expression {
    public final Token token;
    public Expression condition;
    public BlockStatement body;

    public ForExpression(Token token) {
        this.token = token;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return "for" + condition + " " + body;
    }
}
