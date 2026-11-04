package catcompiler.ast;

import catcompiler.token.Token;

/**
 * Dos formas:
 *   for (<condición>) { <cuerpo> }                         (igual que while)
 *   for (<init>; <condición>; <actualización>) { <cuerpo> } (estilo C)
 * En la primera forma init y update quedan en null.
 */
public class ForExpression implements Expression {
    public final Token token;
    public Statement init;
    public Expression condition;
    public Expression update;
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
        if (init == null) {
            return "for" + condition + " " + body;
        }
        return "for(" + init + " " + condition + "; " + update + ") " + body;
    }
}
