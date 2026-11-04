package catcompiler.object;

import catcompiler.token.Token;

/**
 * Error como VALOR (no como excepción de Java). Cada paso del evaluador
 * revisa si recibió un ErrorObj y, si es así, lo devuelve sin seguir: el error
 * "burbujea" hasta arriba y detiene la ejecución. message ya trae el tipo de
 * error, la línea y la columna.
 */
public record ErrorObj(String message) implements CatObject {

    /** Error de ejecución ubicado en el token donde ocurrió. */
    public static ErrorObj at(Token token, String detail) {
        return new ErrorObj("Error de ejecución " + token.position() + ": " + detail);
    }

    @Override
    public String type() {
        return "error";
    }

    @Override
    public String inspect() {
        return message;
    }
}
