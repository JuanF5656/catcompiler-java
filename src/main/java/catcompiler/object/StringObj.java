package catcompiler.object;

/** Texto inmutable. Es un record para que compare por valor (sirve como clave). */
public record StringObj(String value) implements Hashable {
    @Override
    public String type() {
        return "string";
    }

    @Override
    public String inspect() {
        return "\"" + value + "\"";
    }
}
