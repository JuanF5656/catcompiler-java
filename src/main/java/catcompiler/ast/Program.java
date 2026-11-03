package catcompiler.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodo RAÍZ del árbol: un programa es una lista de sentencias.
 */
public class Program implements Node {
    public final List<Statement> statements = new ArrayList<>();

    @Override
    public String tokenLiteral() {
        return statements.isEmpty() ? "" : statements.get(0).tokenLiteral();
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (Statement s : statements) {
            out.append(s);
        }
        return out.toString();
    }
}
