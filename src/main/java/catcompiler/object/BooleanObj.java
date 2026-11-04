package catcompiler.object;

/**
 * Solo existen dos booleanos (TRUE y FALSE). Al reutilizar siempre las mismas
 * dos instancias no se crean objetos nuevos en cada comparación y la igualdad
 * se puede revisar por identidad.
 */
public final class BooleanObj implements Hashable {
    public static final BooleanObj TRUE = new BooleanObj(true);
    public static final BooleanObj FALSE = new BooleanObj(false);

    public final boolean value;

    private BooleanObj(boolean value) {
        this.value = value;
    }

    public static BooleanObj of(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public String type() {
        return "booleano";
    }

    @Override
    public String inspect() {
        return Boolean.toString(value);
    }
}
