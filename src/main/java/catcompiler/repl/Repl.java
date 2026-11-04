package catcompiler.repl;

import catcompiler.ast.Program;
import catcompiler.interpreter.Interpreter;
import catcompiler.lexer.Lexer;
import catcompiler.object.CatObject;
import catcompiler.object.ErrorObj;
import catcompiler.object.NullObj;
import catcompiler.parser.Parser;
import catcompiler.token.Token;
import catcompiler.token.TokenType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * REPL (Read-Eval-Print Loop) con tres modos:
 *  - LEXER_MODE:     imprime los tokens
 *  - PARSER_MODE:    construye e imprime el AST
 *  - EVALUATOR_MODE: ejecuta el código e imprime el resultado
 *
 * Si una entrada deja llaves, paréntesis o corchetes abiertos, se siguen
 * leyendo líneas (prompt "..") para poder escribir funciones de varias líneas.
 */
public class Repl {
    public static final String PROMPT = ">> ";
    public static final String CONTINUATION_PROMPT = ".. ";

    public static final int LEXER_MODE = 0;
    public static final int PARSER_MODE = 1;
    public static final int EVALUATOR_MODE = 2;

    public static void start(BufferedReader in, PrintStream out, int mode) throws IOException {
        // Un solo intérprete para toda la sesión: las variables sobreviven entre líneas
        Interpreter interpreter = new Interpreter(out, in);
        while (true) {
            String source = readInput(in, out);
            if (source == null) {
                return;
            }
            String cleaned = source.trim().toLowerCase();
            if (cleaned.equals("exit") || cleaned.equals("salir")) {
                out.println("¡Miau! Nos vemos. Saliendo de CatCompiler...");
                return;
            }

            switch (mode) {
                case LEXER_MODE -> printTokens(out, source);
                case PARSER_MODE -> printAst(out, source);
                default -> printResult(out, interpreter.run(source));
            }
        }
    }

    /** Lee una entrada completa (una o varias líneas). Devuelve null al final de la entrada. */
    private static String readInput(BufferedReader in, PrintStream out) throws IOException {
        out.print(PROMPT);
        out.flush();
        String line = in.readLine();
        if (line == null) {
            return null;
        }
        StringBuilder source = new StringBuilder(line);
        while (openDelimiters(source.toString()) > 0) {
            out.print(CONTINUATION_PROMPT);
            out.flush();
            String next = in.readLine();
            if (next == null) {
                break;
            }
            source.append('\n').append(next);
        }
        return source.toString();
    }

    /** Cuenta ( [ { abiertos usando el propio Lexer, así un "{" dentro de un string no cuenta. */
    private static int openDelimiters(String source) {
        Lexer lexer = new Lexer(source);
        int depth = 0;
        for (Token tok = lexer.nextToken(); tok.type() != TokenType.EOF; tok = lexer.nextToken()) {
            switch (tok.type()) {
                case LPAREN, LBRACKET, LBRACE -> depth++;
                case RPAREN, RBRACKET, RBRACE -> depth--;
                default -> { }
            }
        }
        return depth;
    }

    private static void printTokens(PrintStream out, String source) {
        Lexer lexer = new Lexer(source);
        for (Token tok = lexer.nextToken(); tok.type() != TokenType.EOF; tok = lexer.nextToken()) {
            out.println(tok);
        }
        lexer.errors().forEach(out::println);
    }

    private static void printAst(PrintStream out, String source) {
        Parser parser = new Parser(new Lexer(source));
        Program program = parser.parseProgram();
        if (!parser.errors().isEmpty()) {
            out.println("¡Miau! Ocurrió un error al intentar parsear el código:");
            parser.errors().forEach(msg -> out.println("\t- " + msg));
            return;
        }
        out.println("AST Generado: ");
        out.println(program);
    }

    /** Muestra el valor final; null no se imprime para no ensuciar la salida de let o print. */
    private static void printResult(PrintStream out, CatObject result) {
        if (result instanceof ErrorObj error) {
            out.println(error.message());
        } else if (result != NullObj.NULL) {
            out.println(result.inspect());
        }
    }
}
