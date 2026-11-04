package catcompiler.object;

/**
 * Envoltorio que marca "aquí hubo un return". Mientras sube por los bloques
 * nadie lo desenvuelve, así un return dentro de un if o un while sale de toda
 * la función. Solo la llamada a la función (o el programa) lo abre.
 */
public record ReturnValue(CatObject value) implements CatObject {
    @Override
    public String type() {
        return "return";
    }

    @Override
    public String inspect() {
        return value.inspect();
    }
}
