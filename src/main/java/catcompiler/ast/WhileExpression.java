package catcompiler.ast;

import catcompiler.token.Token;

/**
 * while (<condición>) { <cuerpo> }
 */
public class WhileExpression implements Expression {
    public final Token token;
    public Expression condition;
    public BlockStatement body;

    public WhileExpression(Token token) {
        this.token = token;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return "while" + condition + " " + body;
    }
}
