package catcompiler.object;

import catcompiler.token.Token;

/**
 * Señal de "break": sube por los bloques igual que ReturnValue hasta que la
 * atrapa el ciclo más cercano. Guarda el token para poder reportar un break
 * fuera de un ciclo en la línea correcta.
 */
public record BreakSignal(Token token) implements CatObject {
    @Override
    public String type() {
        return "break";
    }

    @Override
    public String inspect() {
        return "break";
    }
}
