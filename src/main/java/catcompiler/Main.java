package catcompiler;

import catcompiler.interpreter.Interpreter;
import catcompiler.object.CatObject;
import catcompiler.object.ErrorObj;
import catcompiler.repl.Repl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dos formas de uso:
 *   java -cp out catcompiler.Main                 -> menú + REPL
 *   java -cp out catcompiler.Main programa.cat    -> ejecuta el archivo
 */
public class Main {
    public static void main(String[] args) throws IOException {
        // Un solo lector para el menú, el REPL e input(): si hubiera dos, el
        // primero podría quedarse con texto en su buffer que el otro no vería.
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        if (args.length == 0) {
            startRepl(in);
            return;
        }
        int exitCode = runFile(args[0], in);
        // Solo salimos explícitamente si hubo error: con mvn exec:java el programa corre
        // dentro de la JVM de Maven y un System.exit(0) cortaría también a Maven.
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /** Ejecuta un archivo .cat. Devuelve el código de salida del proceso (0 = sin errores). */
    private static int runFile(String path, BufferedReader in) {
        String source;
        try {
            source = Files.readString(Path.of(path));
        } catch (IOException e) {
            System.out.println("No se pudo leer el archivo: " + path);
            return 2;
        }
        CatObject result = new Interpreter(System.out, in).run(source);
        if (result instanceof ErrorObj error) {
            System.out.println(error.message());
            return 1;
        }
        return 0;
    }

    private static void startRepl(BufferedReader in) throws IOException {
        String username = System.getProperty("user.name");

        System.out.print("********************************************\n");
        System.out.print("**                                        **\n");
        System.out.print("**  -- BIENVENIDO A CATCOMPILER REPL --   **\n");
        System.out.print("**                                        **\n");
        System.out.print("**      /\\_/\\     ¡Miau! Soy Limón,       **\n");
        System.out.print("**     ( o.o )    tu asistente felino.    **\n");
        System.out.print("**      > ^ <     ¡Dame esos tokens!      **\n");
        System.out.print("**                                        **\n");
        System.out.print("********************************************\n\n");
        System.out.printf("¡Hola %s! Estás en CatCompiler.%n", username);

        System.out.print("\nSelecciona el modo de interacción:\n");
        System.out.print("1. Modo Lexer (Muestra los Tokens generados)\n");
        System.out.print("2. Modo Parser (Construye y muestra el Árbol AST)\n");
        System.out.print("3. Modo Evaluador (Ejecuta el código)\n");
        System.out.print("Opción [1, 2 o 3]: ");
        System.out.flush();

        String option = in.readLine();
        option = option == null ? "" : option.trim();

        int mode;
        switch (option) {
            case "2" -> {
                mode = Repl.PARSER_MODE;
                System.out.print("\n¡Iniciando en Modo Parser! Escribe código para ver el árbol.\n");
            }
            case "3" -> {
                mode = Repl.EVALUATOR_MODE;
                System.out.print("\n¡Iniciando en Modo Evaluador! Escribe código para ejecutarlo.\n");
            }
            default -> {
                mode = Repl.LEXER_MODE;
                System.out.print("\n¡Iniciando en Modo Lexer! Escribe código para ver los tokens.\n");
            }
        }

        Repl.start(in, System.out, mode);
    }
}
