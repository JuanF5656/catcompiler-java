package catcompiler.object;

/** "Ningún valor": lo producen let, print, un if sin else que no se cumple, etc. */
public final class NullObj implements CatObject {
    public static final NullObj NULL = new NullObj();

    private NullObj() {
    }

    @Override
    public String type() {
        return "nulo";
    }

    @Override
    public String inspect() {
        return "null";
    }
}
