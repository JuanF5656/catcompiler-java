package catcompiler.ast;

import catcompiler.token.Token;

/**
 * Nombre de una variable o función (ej. x, edad). Es una hoja del árbol.
 */
public class Identifier implements Expression {
    public final Token token;   // el token IDENT
    public final String value;  // el nombre

    public Identifier(Token token, String value) {
        this.token = token;
        this.value = value;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return value;
    }
}
