package catcompiler.parser;

import catcompiler.ast.Program;
import catcompiler.lexer.Lexer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {

    private String parse(String input) {
        Parser p = new Parser(new Lexer(input));
        Program program = p.parseProgram();
        assertTrue(p.errors().isEmpty(), "errores: " + p.errors());
        return program.toString();
    }

    @Test
    void testPrecedencia() {
        String[][] tests = {
                {"3 + 4 <= 4", "((3 + 4) <= 4)"},
                {"1 + 2 * 3", "(1 + (2 * 3))"},
                {"-a * b", "((-a) * b)"},
                {"!-a", "(!(-a))"},
                {"a + b - c", "((a + b) - c)"},
                {"2 * 3 ** 2", "(2 * (3 ** 2))"},
                {"(1 + 2) * 3", "((1 + 2) * 3)"},
                {"5 > 4 == 3 < 4", "((5 > 4) == (3 < 4))"},
                {"a + add(b * c) + d", "((a + add((b * c))) + d)"},
                {"add(a, b, 1, 2 * 3, 4 + 5, add(6, 7 * 8))",
                        "add(a, b, 1, (2 * 3), (4 + 5), add(6, (7 * 8)))"},
        };
        for (String[] t : tests) {
            assertEquals(t[1], parse(t[0]), "input: " + t[0]);
        }
    }

    @Test
    void testSentencias() {
        assertEquals("let x = (5 + 3);", parse("let x = 5 + 3;"));
        assertEquals("return (x * 2);", parse("return x * 2;"));
        assertEquals("if(x < y) xelse y", parse("if (x < y) { x } else { y }"));
        assertEquals("function(x, y) (x + y)", parse("function(x, y) { x + y; }"));
        assertEquals("while(i < 10) let i = (i + 1);", parse("while (i < 10) { let i = i + 1; }"));
    }

    @Test
    void testPrecedenciaExtendida() {
        String[][] tests = {
                {"a || b && c", "(a || (b && c))"},
                {"a && b == c", "(a && (b == c))"},
                {"a == b < c", "(a == (b < c))"},
                {"a < b + c", "(a < (b + c))"},
                {"a + b % c", "(a + (b % c))"},
                {"a * b ** c", "(a * (b ** c))"},
                {"2 ** 3 ** 2", "(2 ** (3 ** 2))"},
                {"-2 ** 2", "((-2) ** 2)"},
                {"-a[0]", "(-(a[0]))"},
                {"f(x)[1]", "(f(x)[1])"},
                {"x = y = 1 + 2", "(x = (y = (1 + 2)))"},
                {"a[i] = a[i] * 2", "((a[i]) = ((a[i]) * 2))"},
                {"x = a || b", "(x = (a || b))"},
        };
        for (String[] t : tests) {
            assertEquals(t[1], parse(t[0]), "input: " + t[0]);
        }
    }

    @Test
    void testLiterales() {
        assertEquals("\"hola\"", parse("\"hola\""));
        assertEquals("[1, (2 + 3), \"a\"]", parse("[1, 2 + 3, \"a\"]"));
        assertEquals("{\"a\": 1, 2: true}", parse("{\"a\": 1, 2: true}"));
        assertEquals("{}", parse("{}"));
    }

    @Test
    void testElifForBreak() {
        assertEquals("if(a > 1) 1else if(b > 1) 2else 3", parse("if (a > 1) { 1 } elif (b > 1) { 2 } else { 3 }"));
        assertEquals("for(let i = 0; (i < 3); (i = (i + 1))) break;",
                parse("for (let i = 0; i < 3; i = i + 1) { break; }"));
        assertEquals("for(i < 3) (i = (i + 1))", parse("for (i < 3) { i = i + 1 }"));
        assertEquals("function fib(n) return n;", parse("function fib(n) { return n; }"));
    }

    @Test
    void testBloqueAlFinalDeLineaCortaLaExpresion() {
        // Sin la regla, la segunda línea se leería como una resta: (if... - 1)
        Parser p = new Parser(new Lexer("if (a > 1) { 1 }\n-1"));
        assertEquals(2, p.parseProgram().statements.size());
        // En la misma línea sí sigue siendo una sola expresión
        assertEquals("(if(a > 1) 1 - 1)", parse("if (a > 1) { 1 } - 1"));
    }

    @Test
    void testErrores() {
        Parser p = new Parser(new Lexer("let = 5;"));
        p.parseProgram();
        assertTrue(!p.errors().isEmpty());

        assertSyntaxError("let x = (5 + 3;", "Error sintáctico [línea 1, col 15]: se esperaba ')' pero se encontró ';'");
        assertSyntaxError("let = 5;", "Error sintáctico [línea 1, col 5]: se esperaba un identificador pero se encontró '='");
        assertSyntaxError("if (x) {\n  y", "Error sintáctico [línea 2, col 4]: se esperaba '}' pero se encontró el fin del archivo");
        assertSyntaxError("5 = 3", "Error sintáctico [línea 1, col 3]: no se puede asignar a 5; solo a una variable o a lista[i]");
        assertSyntaxError("let x = ;", "Error sintáctico [línea 1, col 9]: expresión inválida, no se esperaba ';'");
        // Si hay un error léxico, se reporta ese y no el sintáctico que provoca
        assertSyntaxError("let x = @;", "Error léxico [línea 1, col 9]: carácter ilegal '@'");
    }

    private void assertSyntaxError(String input, String expected) {
        Parser p = new Parser(new Lexer(input));
        p.parseProgram();
        assertEquals(List.of(expected), p.errors(), "input: " + input);
    }
}
