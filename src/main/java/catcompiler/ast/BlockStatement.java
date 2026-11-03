package catcompiler.ast;

import catcompiler.token.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Bloque de código entre llaves { ... } (if, else, while, for, funciones).
 */
public class BlockStatement implements Statement {
    public final Token token;   // el token {
    public final List<Statement> statements = new ArrayList<>();

    public BlockStatement(Token token) {
        this.token = token;
    }

    @Override
    public String tokenLiteral() {
        return token.literal();
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
