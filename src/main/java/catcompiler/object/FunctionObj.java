package catcompiler.object;

import catcompiler.ast.BlockStatement;
import catcompiler.ast.Identifier;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Función definida por el usuario. Guarda el Environment donde se CREÓ (no
 * donde se llama): eso es lo que hace posibles los closures, porque la función
 * sigue viendo las variables de ese entorno aunque ya se haya salido de él.
 */
public record FunctionObj(List<Identifier> parameters, BlockStatement body, Environment env)
        implements CatObject {

    @Override
    public String type() {
        return "funcion";
    }

    @Override
    public String inspect() {
        return "function(" + parameters.stream().map(Identifier::toString).collect(Collectors.joining(", ")) + ")";
    }
}
