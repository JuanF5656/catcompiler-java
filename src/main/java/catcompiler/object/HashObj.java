package catcompiler.object;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Diccionario. LinkedHashMap conserva el orden de inserción, así keys() y la
 * impresión salen en el mismo orden en que se escribieron (salida predecible).
 */
public final class HashObj implements CatObject {
    public final Map<Hashable, CatObject> pairs = new LinkedHashMap<>();

    @Override
    public String type() {
        return "diccionario";
    }

    @Override
    public String inspect() {
        return "{" + pairs.entrySet().stream()
                .map(e -> e.getKey().inspect() + ": " + e.getValue().inspect())
                .collect(Collectors.joining(", ")) + "}";
    }
}
