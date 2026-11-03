package catcompiler.parser;

import catcompiler.ast.Program;
import catcompiler.lexer.Lexer;
import org.junit.jupiter.api.Test;

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
    void testErrores() {
        Parser p = new Parser(new Lexer("let = 5;"));
        p.parseProgram();
        assertTrue(!p.errors().isEmpty());
    }
}
