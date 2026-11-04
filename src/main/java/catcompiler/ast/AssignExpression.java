package catcompiler.ast;

import catcompiler.token.Token;

/**
 * Reasignación de algo que ya existe:  x = valor   o   lista[i] = valor
 * Es una expresión (no sentencia) para que funcione dentro de un for:
 * for (let i = 0; i < 3; i = i + 1) { ... }
 */
public class AssignExpression implements Expression {
    public final Token token;        // el token =
    public final Expression target;  // Identifier o IndexExpression (lo valida el parser)
    public Expression value;

    public AssignExpression(Token token, Expression target) {
        this.token = token;
        this.target = target;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return "(" + target + " = " + value + ")";
    }
}
