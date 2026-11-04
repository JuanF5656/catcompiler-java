package catcompiler.object;

/** Entero de 64 bits. Es un record para que compare por valor (sirve como clave). */
public record IntegerObj(long value) implements Hashable {
    @Override
    public String type() {
        return "entero";
    }

    @Override
    public String inspect() {
        return Long.toString(value);
    }
}
