package catcompiler.ast;

/**
 * Interfaz raíz: TODO en el AST es un nodo.
 * - tokenLiteral(): el literal del token que originó el nodo (depuración)
 * - toString():     el nodo impreso como texto legible
 */
public interface Node {
    String tokenLiteral();
}
