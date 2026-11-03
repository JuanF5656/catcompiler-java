package catcompiler.ast;

import catcompiler.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Definición de función: function(x, y) { return x + y; }
 */
public class FunctionLiteral implements Expression {
    public final Token token;
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
        return tokenLiteral() + "(" + params + ") " + body;
    }
}
