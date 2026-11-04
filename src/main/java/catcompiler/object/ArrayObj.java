package catcompiler.object;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Lista mutable: lista[i] = v y push() la modifican en el lugar. */
public final class ArrayObj implements CatObject {
    public final List<CatObject> elements;

    public ArrayObj(List<? extends CatObject> elements) {
        this.elements = new ArrayList<>(elements);
    }

    @Override
    public String type() {
        return "lista";
    }

    @Override
    public String inspect() {
        return "[" + elements.stream().map(CatObject::inspect).collect(Collectors.joining(", ")) + "]";
    }
}
