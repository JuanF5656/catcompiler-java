package catcompiler.ast;

import catcompiler.token.Token;

/**
 * if (<condición>) { <consecuencia> } else { <alternativa> }
 * alternative puede ser null si no hay else. Un elif se representa como un
 * alternative que contiene otro IfExpression (ver Parser.parseIfExpression).
 */
public class IfExpression implements Expression {
    public final Token token;
    public Expression condition;
    public BlockStatement consequence;
    public BlockStatement alternative;

    public IfExpression(Token token) {
        this.token = token;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("if").append(condition).append(" ").append(consequence);
        if (alternative != null) {
            out.append("else ").append(alternative);
        }
        return out.toString();
    }
}
