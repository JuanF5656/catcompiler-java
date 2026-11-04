package catcompiler.object;

/**
 * Marca los valores que pueden ser clave de un diccionario (entero, string y
 * booleano). Son inmutables y comparan por valor, así que su equals/hashCode
 * sirven directamente como clave de un HashMap de Java. Una lista no puede ser
 * clave: si cambiara después de insertarla, su hash dejaría de coincidir.
 */
public interface Hashable extends CatObject {
}
