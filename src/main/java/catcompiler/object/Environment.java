package catcompiler.object;

import java.util.HashMap;
import java.util.Map;

/**
 * Ámbito de variables: un mapa nombre -> valor más una referencia al ámbito
 * que lo contiene (outer). Los ámbitos forman una cadena: bloque -> función
 * -> ... -> global (outer == null). Buscar una variable es recorrer esa cadena
 * hacia afuera, por eso la definición más cercana "sombrea" a las de afuera.
 */
public class Environment {
    private final Map<String, CatObject> store = new HashMap<>();
    private final Environment outer;

    /** Ámbito global. */
    public Environment() {
        this(null);
    }

    /** Ámbito anidado (bloque o llamada a función). */
    public Environment(Environment outer) {
        this.outer = outer;
    }

    /** Busca desde este ámbito hacia afuera. Devuelve null si no existe en ninguno. */
    public CatObject get(String name) {
        for (Environment env = this; env != null; env = env.outer) {
            CatObject value = env.store.get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /** let: declara SIEMPRE en el ámbito actual (si existe afuera, lo sombrea). */
    public void define(String name, CatObject value) {
        store.put(name, value);
    }

    /**
     * x = valor: modifica la variable en el ámbito más cercano donde ya existe.
     * Devuelve false si no existe en ninguno: asignar no crea variables, para
     * que un error de tipeo no invente una variable nueva en silencio.
     */
    public boolean assign(String name, CatObject value) {
        for (Environment env = this; env != null; env = env.outer) {
            if (env.store.containsKey(name)) {
                env.store.put(name, value);
                return true;
            }
        }
        return false;
    }
}
