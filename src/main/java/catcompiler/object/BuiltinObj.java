package catcompiler.object;

/** Función integrada (print, len, ...) vista como un valor más del lenguaje. */
public record BuiltinObj(String name, BuiltinFunction function) implements CatObject {
    @Override
    public String type() {
        return "builtin";
    }

    @Override
    public String inspect() {
        return "<builtin " + name + ">";
    }
}
