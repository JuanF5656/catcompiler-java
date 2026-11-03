package catcompiler.ast;

import catcompiler.token.Token;

/**
 * Operador ANTES del operando: -5, !true. Se imprime como (-5).
 */
public class PrefixExpression implements Expression {
    public final Token token;
    public final String operator;  // "-" o "!"
    public Expression right;

    public PrefixExpression(Token token, String operator) {
        this.token = token;
        this.operator = operator;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return "(" + operator + right + ")";
    }
}
