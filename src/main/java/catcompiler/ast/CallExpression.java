package catcompiler.ast;

import catcompiler.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Llamada a función: add(2, 3)
 */
public class CallExpression implements Expression {
    public final Token token;          // el token (
    public final Expression function;  // Identifier o FunctionLiteral
    public List<Expression> arguments = new ArrayList<>();

    public CallExpression(Token token, Expression function) {
        this.token = token;
        this.function = function;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        String args = arguments.stream().map(String::valueOf).collect(Collectors.joining(", "));
        return function + "(" + args + ")";
    }
}
