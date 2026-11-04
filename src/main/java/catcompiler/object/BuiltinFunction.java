package catcompiler.object;

import catcompiler.token.Token;

import java.util.List;

/**
 * Implementación en Java de una función integrada. Recibe el token de la
 * llamada para que sus errores también tengan línea y columna.
 */
@FunctionalInterface
public interface BuiltinFunction {
    CatObject apply(Token callToken, List<CatObject> args);
}
