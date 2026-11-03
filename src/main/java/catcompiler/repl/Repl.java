package catcompiler.repl;

import catcompiler.ast.Program;
import catcompiler.lexer.Lexer;
import catcompiler.parser.Parser;
import catcompiler.token.Token;
import catcompiler.token.TokenType;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * REPL (Read-Eval-Print Loop) con dos modos:
 *  - LEXER_MODE:  imprime los tokens
 *  - PARSER_MODE: construye e imprime el AST
 */
public class Repl {
    public static final String PROMPT = ">> ";

    public static final int LEXER_MODE = 0;
    public static final int PARSER_MODE = 1;

    public static void start(Scanner scanner, PrintStream out, int mode) {
        while (true) {
            out.print(PROMPT);
            out.flush();

            if (!scanner.hasNextLine()) {
                return;
            }
            String line = scanner.nextLine();

            String cleanedLine = line.trim().toLowerCase();
            if (cleanedLine.equals("exit") || cleanedLine.equals("salir")) {
                out.println("¡Miau! Nos vemos. Saliendo de CatCompiler...");
                return;
            }

            Lexer l = new Lexer(line);

            if (mode == LEXER_MODE) {
                // Modo 1: solo imprimir tokens
                for (Token tok = l.nextToken(); tok.type() != TokenType.EOF; tok = l.nextToken()) {
                    out.println(tok);
                }
            } else {
                // Modo 2: construir e imprimir el AST
                Parser p = new Parser(l);
                Program program = p.parseProgram();

                if (!p.errors().isEmpty()) {
                    printParserErrors(out, p.errors());
                    continue;
                }

                out.println("AST Generado: ");
                out.println(program);
            }
        }
    }

    private static void printParserErrors(PrintStream out, List<String> errors) {
        out.println("¡Miau! Ocurrió un error al intentar parsear el código:");
        for (String msg : errors) {
            out.println("\t- " + msg);
        }
    }
}
