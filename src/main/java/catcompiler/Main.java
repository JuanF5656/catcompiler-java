package catcompiler;

import catcompiler.repl.Repl;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
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
        System.out.print("Opción [1 o 2]: ");
        System.out.flush();

        // Un solo Scanner para el menú y el REPL (así no se pierde entrada)
        Scanner scanner = new Scanner(System.in);
        String option = scanner.hasNextLine() ? scanner.nextLine().trim() : "";

        int mode = Repl.LEXER_MODE;
        if (option.equals("2")) {
            mode = Repl.PARSER_MODE;
            System.out.print("\n¡Iniciando en Modo Parser! Escribe código para ver el árbol.\n");
        } else {
            System.out.print("\n¡Iniciando en Modo Lexer! Escribe código para ver los tokens.\n");
        }

        Repl.start(scanner, System.out, mode);
    }
}
