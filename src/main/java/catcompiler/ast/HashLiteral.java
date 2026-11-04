package catcompiler.ast;

import catcompiler.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Diccionario literal: {"nombre": "Limón", "edad": 3}
 * Se guarda como lista de pares (y no como Map) para conservar el orden en que
 * se escribieron y para no exigir aquí que las claves sean hashables: eso solo
 * se sabe al evaluar.
 */
public class HashLiteral implements Expression {
    public record Pair(Expression key, Expression value) {
        @Override
        public String toString() {
            return key + ": " + value;
        }
    }

    public final Token token;   // el token {
    public final List<Pair> pairs = new ArrayList<>();

    public HashLiteral(Token token) {
        this.token = token;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
    }

    @Override
    public String toString() {
        return "{" + pairs.stream().map(Pair::toString).collect(Collectors.joining(", ")) + "}";
    }
}
