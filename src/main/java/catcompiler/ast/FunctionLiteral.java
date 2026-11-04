package catcompiler.ast;

import catcompiler.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Definición de función:
 *   function(x, y) { return x + y; }        (anónima, se asigna con let)
 *   function suma(x, y) { return x + y; }   (con nombre: además la declara)
 * name es null en las anónimas.
 */
public class FunctionLiteral implements Expression {
    public final Token token;
    public Identifier name;
    public List<Identifier> parameters = new ArrayList<>();
    public BlockStatement body;

    public FunctionLiteral(Token token) {
        this.token = token;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        String params = parameters.stream().map(Object::toString).collect(Collectors.joining(", "));
        String header = name == null ? tokenLiteral() : tokenLiteral() + " " + name;
        return header + "(" + params + ") " + body;
    }
}
