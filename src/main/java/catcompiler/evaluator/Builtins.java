package catcompiler.evaluator;

import catcompiler.object.*;
import catcompiler.token.Token;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static catcompiler.object.NullObj.NULL;

/**
 * Funciones integradas: print, len, str, int, type, push, first, last, rest,
 * keys e input. Cada una valida cuántos argumentos recibe y de qué tipo, y
 * devuelve un ErrorObj (con la posición de la llamada) si algo no cuadra.
 *
 * Recibe la salida y la entrada por constructor (en vez de usar System.out
 * directo) para que las pruebas puedan capturar lo que imprime print().
 */
public class Builtins {
    private final Map<String, BuiltinObj> functions = new HashMap<>();
    private final PrintStream out;
    private final BufferedReader in;

    public Builtins(PrintStream out, BufferedReader in) {
        this.out = out;
        this.in = in;
        register("print", this::print);
        register("input", this::input);
        register("len", Builtins::len);
        register("str", Builtins::str);
        register("int", Builtins::toInt);
        register("type", Builtins::type);
        register("push", Builtins::push);
        register("first", Builtins::first);
        register("last", Builtins::last);
        register("rest", Builtins::rest);
        register("keys", Builtins::keys);
    }

    private void register(String name, BuiltinFunction function) {
        functions.put(name, new BuiltinObj(name, function));
    }

    /** null si no existe una integrada con ese nombre. */
    public BuiltinObj get(String name) {
        return functions.get(name);
    }

    // ========================= ENTRADA / SALIDA =========================

    /** print(a, b, ...): imprime los valores separados por espacio. Acepta 0 o más. */
    private CatObject print(Token call, List<CatObject> args) {
        out.println(args.stream().map(Builtins::display).collect(Collectors.joining(" ")));
        return NULL;
    }

    /** input() o input("mensaje"): lee una línea. Al final de la entrada devuelve "". */
    private CatObject input(Token call, List<CatObject> args) {
        if (args.size() > 1) {
            return wrongCount(call, "input", "0 o 1 argumento", args.size());
        }
        if (args.size() == 1) {
            out.print(display(args.get(0)));
            out.flush();
        }
        try {
            String line = in.readLine();
            return new StringObj(line == null ? "" : line);
        } catch (IOException e) {
            return ErrorObj.at(call, "input() no pudo leer la entrada: " + e.getMessage());
        }
    }

    // ============================ CONVERSIÓN ============================

    private static CatObject len(Token call, List<CatObject> args) {
        ErrorObj error = expectCount(call, "len", args, 1);
        if (error != null) {
            return error;
        }
        CatObject arg = args.get(0);
        if (arg instanceof StringObj s) return new IntegerObj(s.value().length());
        if (arg instanceof ArrayObj a) return new IntegerObj(a.elements.size());
        if (arg instanceof HashObj h) return new IntegerObj(h.pairs.size());
        return wrongType(call, "len", arg);
    }

    private static CatObject str(Token call, List<CatObject> args) {
        ErrorObj error = expectCount(call, "str", args, 1);
        return error != null ? error : new StringObj(display(args.get(0)));
    }

    private static CatObject toInt(Token call, List<CatObject> args) {
        ErrorObj error = expectCount(call, "int", args, 1);
        if (error != null) {
            return error;
        }
        CatObject arg = args.get(0);
        if (arg instanceof IntegerObj) return arg;
        if (arg instanceof BooleanObj b) return new IntegerObj(b.value ? 1 : 0);
        if (arg instanceof StringObj s) return parseInt(call, s.value());
        return wrongType(call, "int", arg);
    }

    private static CatObject parseInt(Token call, String text) {
        try {
            return new IntegerObj(Long.parseLong(text.trim()));
        } catch (NumberFormatException e) {
            return ErrorObj.at(call, "int() no puede convertir \"" + text + "\" a entero");
        }
    }

    private static CatObject type(Token call, List<CatObject> args) {
        ErrorObj error = expectCount(call, "type", args, 1);
        return error != null ? error : new StringObj(args.get(0).type());
    }

    // ============================== LISTAS ==============================

    /** push(lista, valor): agrega al final MODIFICANDO la lista, y la devuelve. */
    private static CatObject push(Token call, List<CatObject> args) {
        ErrorObj error = expectList(call, "push", args, 2);
        if (error != null) {
            return error;
        }
        ArrayObj list = (ArrayObj) args.get(0);
        list.elements.add(args.get(1));
        return list;
    }

    private static CatObject first(Token call, List<CatObject> args) {
        ErrorObj error = expectNonEmptyList(call, "first", args);
        return error != null ? error : ((ArrayObj) args.get(0)).elements.get(0);
    }

    private static CatObject last(Token call, List<CatObject> args) {
        ErrorObj error = expectNonEmptyList(call, "last", args);
        if (error != null) {
            return error;
        }
        List<CatObject> elements = ((ArrayObj) args.get(0)).elements;
        return elements.get(elements.size() - 1);
    }

    /** rest(lista): una lista NUEVA sin el primer elemento (la original no cambia). */
    private static CatObject rest(Token call, List<CatObject> args) {
        ErrorObj error = expectNonEmptyList(call, "rest", args);
        if (error != null) {
            return error;
        }
        List<CatObject> elements = ((ArrayObj) args.get(0)).elements;
        return new ArrayObj(elements.subList(1, elements.size()));
    }

    // =========================== DICCIONARIOS ===========================

    /** keys(dicc): lista con las claves en orden de inserción. */
    private static CatObject keys(Token call, List<CatObject> args) {
        ErrorObj error = expectCount(call, "keys", args, 1);
        if (error != null) {
            return error;
        }
        if (!(args.get(0) instanceof HashObj hash)) {
            return wrongType(call, "keys", args.get(0));
        }
        return new ArrayObj(List.copyOf(hash.pairs.keySet()));
    }

    // ============================ VALIDACIÓN ============================

    private static ErrorObj expectCount(Token call, String name, List<CatObject> args, int expected) {
        return args.size() == expected ? null : wrongCount(call, name, count(expected), args.size());
    }

    /** Valida cantidad de argumentos y que el primero sea una lista. */
    private static ErrorObj expectList(Token call, String name, List<CatObject> args, int expected) {
        ErrorObj error = expectCount(call, name, args, expected);
        if (error == null && !(args.get(0) instanceof ArrayObj)) {
            error = wrongType(call, name, args.get(0));
        }
        return error;
    }

    private static ErrorObj expectNonEmptyList(Token call, String name, List<CatObject> args) {
        ErrorObj error = expectList(call, name, args, 1);
        if (error == null && ((ArrayObj) args.get(0)).elements.isEmpty()) {
            error = ErrorObj.at(call, name + "() recibió una lista vacía");
        }
        return error;
    }

    private static ErrorObj wrongCount(Token call, String name, String expected, int received) {
        return ErrorObj.at(call, name + "() esperaba " + expected + " pero recibió " + received);
    }

    private static ErrorObj wrongType(Token call, String name, CatObject arg) {
        return ErrorObj.at(call, name + "() no acepta un argumento de tipo " + arg.type());
    }

    /** "1 argumento", "2 argumentos": también lo usa el Evaluator para las funciones del usuario. */
    static String count(int n) {
        return n + (n == 1 ? " argumento" : " argumentos");
    }

    /** Cómo se ve un valor al imprimirlo: los strings sin comillas, el resto como inspect(). */
    private static String display(CatObject value) {
        return value instanceof StringObj s ? s.value() : value.inspect();
    }
}
