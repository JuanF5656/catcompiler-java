package catcompiler.ast;

import catcompiler.token.Token;

/**
 * Retorno:  return <expresión>;
 */
public class ReturnStatement implements Statement {
    public final Token token;       // el token RETURN
    public Expression returnValue;  // lo que se retorna

    public ReturnStatement(Token token) {
        this.token = token;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return tokenLiteral() + " " + (returnValue != null ? returnValue : "") + ";";
    }
}
