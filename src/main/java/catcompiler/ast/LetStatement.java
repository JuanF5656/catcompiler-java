package catcompiler.ast;

import catcompiler.token.Token;

/**
 * Declaración de variable:  let <nombre> = <expresión>;
 */
public class LetStatement implements Statement {
    public final Token token;   // el token LET
    public Identifier name;     // nombre de la variable
    public Expression value;    // expresión que produce el valor

    public LetStatement(Token token) {
        this.token = token;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return tokenLiteral() + " " + name + " = " + (value != null ? value : "") + ";";
    }
}
