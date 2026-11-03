package catcompiler.ast;

import catcompiler.token.Token;

/**
 * "Envoltorio" para una expresión suelta (ej. `5 + 3;`), porque Program
 * solo acepta sentencias.
 */
public class ExpressionStatement implements Statement {
    public final Token token;      // primer token de la expresión
    public Expression expression;

    public ExpressionStatement(Token token) {
        this.token = token;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return expression != null ? expression.toString() : "";
    }
}
