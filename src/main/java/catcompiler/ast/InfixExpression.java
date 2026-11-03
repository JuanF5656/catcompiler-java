package catcompiler.ast;

import catcompiler.token.Token;

/**
 * Operador ENTRE dos operandos: 5 + 3, x == y.
 * Se imprime con paréntesis para ver la agrupación: 1 + 2 * 3 -> (1 + (2 * 3))
 */
public class InfixExpression implements Expression {
    public final Token token;
    public final Expression left;
    public final String operator;
    public Expression right;

    public InfixExpression(Token token, String operator, Expression left) {
        this.token = token;
        this.operator = operator;
        this.left = left;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return "(" + left + " " + operator + " " + right + ")";
    }
}
