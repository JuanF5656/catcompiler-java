package catcompiler.interpreter;

import catcompiler.ast.Program;
import catcompiler.evaluator.Builtins;
import catcompiler.evaluator.Evaluator;
import catcompiler.lexer.Lexer;
import catcompiler.object.CatObject;
import catcompiler.object.Environment;
import catcompiler.object.ErrorObj;
import catcompiler.parser.Parser;

import java.io.BufferedReader;
import java.io.PrintStream;

/**
 * Une las etapas: texto -> Lexer -> Parser -> AST -> Evaluator -> resultado.
 * La usan el REPL, el modo archivo y las pruebas, así las tres ejecutan el
 * código exactamente de la misma forma.
 *
 * El entorno global vive mientras viva el Interpreter: en el REPL, una
 * variable definida en una línea sigue existiendo en la siguiente.
 */
public class Interpreter {
    private static final long STACK_SIZE = 512L * 1024 * 1024; // 512 MB

    private final Environment globals = new Environment();
    private final Evaluator evaluator;

    public Interpreter(PrintStream out, BufferedReader in) {
        this.evaluator = new Evaluator(new Builtins(out, in));
    }

    /**
     * Ejecuta el código fuente. Devuelve el valor de la última sentencia, o un
     * ErrorObj si hubo un error léxico, sintáctico o de ejecución (el primero).
     */
    public CatObject run(String source) {
        Parser parser = new Parser(new Lexer(source));
        Program program = parser.parseProgram();
        if (!parser.errors().isEmpty()) {
            return new ErrorObj(parser.errors().get(0));
        }
        return evaluateWithLargeStack(program);
    }

    /**
     * Cada llamada a una función del lenguaje usa unos 12 marcos de la pila de
     * Java (evalCall, applyFunction, evalStatements, ...). Con la pila por
     * defecto del hilo principal (~1 MB) la recursión no llegaba ni a 1000
     * niveles, así que evaluamos en un hilo propio con una pila más grande.
     */
    private CatObject evaluateWithLargeStack(Program program) {
        CatObject[] result = new CatObject[1];
        Thread worker = new Thread(null, () -> result[0] = evaluate(program), "evaluador", STACK_SIZE);
        worker.start();
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ErrorObj("Error de ejecución: la ejecución fue interrumpida");
        }
        return result[0];
    }

    private CatObject evaluate(Program program) {
        try {
            return evaluator.eval(program, globals);
        } catch (StackOverflowError e) {
            // Una recursión sin caso base agota incluso la pila grande; la convertimos
            // en un error del lenguaje para que el REPL no se caiga.
            return new ErrorObj("Error de ejecución: recursión demasiado profunda (¿falta el caso base?)");
        }
    }
}
