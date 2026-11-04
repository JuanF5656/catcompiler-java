package catcompiler.ast;

import catcompiler.token.Token;

/**
 * Acceso por índice o clave: lista[0], dicc["clave"], texto[2]
 */
public class IndexExpression implements Expression {
    public final Token token;      // el token [
    public final Expression left;  // lo que se indexa
    public Expression index;

    public IndexExpression(Token token, Expression left) {
        this.token = token;
        this.left = left;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return "(" + left + "[" + index + "])";
    }
}
