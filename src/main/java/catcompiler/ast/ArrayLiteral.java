package catcompiler.ast;

import catcompiler.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lista literal: [1, 2 + 3, "a"]
 */
public class ArrayLiteral implements Expression {
    public final Token token;   // el token [
    public List<Expression> elements = new ArrayList<>();

    public ArrayLiteral(Token token) {
        this.token = token;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return "[" + elements.stream().map(String::valueOf).collect(Collectors.joining(", ")) + "]";
    }
}
