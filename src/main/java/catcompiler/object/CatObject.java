package catcompiler.object;

/**
 * Todo valor que existe en tiempo de ejecución es un CatObject.
 * El evaluador recorre el AST y produce estos objetos: el AST describe el
 * programa, los objetos son lo que el programa calcula.
 */
public interface CatObject {
    /** Nombre del tipo, el mismo que devuelve la función integrada type(). */
    String type();

    /** Representación para mostrar en el REPL (los strings van entre comillas). */
    String inspect();
}
