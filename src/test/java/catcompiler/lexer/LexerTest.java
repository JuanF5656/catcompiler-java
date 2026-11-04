package catcompiler.lexer;

import catcompiler.token.Token;
import catcompiler.token.TokenType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerTest {

    @Test
    void testNextToken() {
        String input = """
let five = 5;
let ten = 10;

function add(x, y) {
  return x + y;
}

let result = add(five, ten);
!-/*5;
5 < 10 > 5;

if (5 < 10) {
	return true;
} else {
	return false;
}

elif {
	break;
}

for while

10 == 10;
10 != 9;
10 <= 10;
10 >= 9;
2 ** 3;
""";

        Object[][] tests = {
                {TokenType.LET, "let"},
                {TokenType.IDENT, "five"},
                {TokenType.ASSIGN, "="},
                {TokenType.INT, "5"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.LET, "let"},
                {TokenType.IDENT, "ten"},
                {TokenType.ASSIGN, "="},
                {TokenType.INT, "10"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.FUNCTION, "function"},
                {TokenType.IDENT, "add"},
                {TokenType.LPAREN, "("},
                {TokenType.IDENT, "x"},
                {TokenType.COMMA, ","},
                {TokenType.IDENT, "y"},
                {TokenType.RPAREN, ")"},
                {TokenType.LBRACE, "{"},
                {TokenType.RETURN, "return"},
                {TokenType.IDENT, "x"},
                {TokenType.PLUS, "+"},
                {TokenType.IDENT, "y"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.RBRACE, "}"},
                {TokenType.LET, "let"},
                {TokenType.IDENT, "result"},
                {TokenType.ASSIGN, "="},
                {TokenType.IDENT, "add"},
                {TokenType.LPAREN, "("},
                {TokenType.IDENT, "five"},
                {TokenType.COMMA, ","},
                {TokenType.IDENT, "ten"},
                {TokenType.RPAREN, ")"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.BANG, "!"},
                {TokenType.MINUS, "-"},
                {TokenType.SLASH, "/"},
                {TokenType.ASTERISK, "*"},
                {TokenType.INT, "5"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.INT, "5"},
                {TokenType.LT, "<"},
                {TokenType.INT, "10"},
                {TokenType.GT, ">"},
                {TokenType.INT, "5"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.IF, "if"},
                {TokenType.LPAREN, "("},
                {TokenType.INT, "5"},
                {TokenType.LT, "<"},
                {TokenType.INT, "10"},
                {TokenType.RPAREN, ")"},
                {TokenType.LBRACE, "{"},
                {TokenType.RETURN, "return"},
                {TokenType.TRUE, "true"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.RBRACE, "}"},
                {TokenType.ELSE, "else"},
                {TokenType.LBRACE, "{"},
                {TokenType.RETURN, "return"},
                {TokenType.FALSE, "false"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.RBRACE, "}"},
                {TokenType.ELIF, "elif"},
                {TokenType.LBRACE, "{"},
                {TokenType.BREAK, "break"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.RBRACE, "}"},
                {TokenType.FOR, "for"},
                {TokenType.WHILE, "while"},
                {TokenType.INT, "10"},
                {TokenType.EQ, "=="},
                {TokenType.INT, "10"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.INT, "10"},
                {TokenType.NOT_EQ, "!="},
                {TokenType.INT, "9"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.INT, "10"},
                {TokenType.LTE, "<="},
                {TokenType.INT, "10"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.INT, "10"},
                {TokenType.GTE, ">="},
                {TokenType.INT, "9"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.INT, "2"},
                {TokenType.POW, "**"},
                {TokenType.INT, "3"},
                {TokenType.SEMICOLON, ";"},
                {TokenType.EOF, ""},
        };

        Lexer l = new Lexer(input);

        for (int i = 0; i < tests.length; i++) {
            Token tok = l.nextToken();
            TokenType expectedType = (TokenType) tests[i][0];
            String expectedLiteral = (String) tests[i][1];

            assertEquals(expectedType, tok.type(),
                    "tests[" + i + "] - tokentype wrong");
            assertEquals(expectedLiteral, tok.literal(),
                    "tests[" + i + "] - literal wrong");
        }
    }

    @Test
    void testStringsOperadoresYComentarios() {
        Lexer l = new Lexer("\"a\\n\\t\\\"\\\\b\" % && || : x1 // comentario\n");
        Object[][] tests = {
                {TokenType.STRING, "a\n\t\"\\b"},
                {TokenType.PERCENT, "%"},
                {TokenType.AND, "&&"},
                {TokenType.OR, "||"},
                {TokenType.COLON, ":"},
                {TokenType.IDENT, "x1"},
                {TokenType.EOF, ""},
        };
        for (Object[] t : tests) {
            Token tok = l.nextToken();
            assertEquals(t[0], tok.type());
            assertEquals(t[1], tok.literal());
        }
        assertTrue(l.errors().isEmpty());
    }

    @Test
    void testLineaYColumna() {
        Lexer l = new Lexer("let x = 5;\n  x == 10");
        int[][] expected = {{1, 1}, {1, 5}, {1, 7}, {1, 9}, {1, 10}, {2, 3}, {2, 5}, {2, 8}};
        for (int[] pos : expected) {
            Token tok = l.nextToken();
            assertEquals(pos[0], tok.line(), "línea de " + tok);
            assertEquals(pos[1], tok.column(), "columna de " + tok);
        }
    }

    @Test
    void testErroresLexicos() {
        assertLexError("let a = 5 @ 3;", "Error léxico [línea 1, col 11]: carácter ilegal '@'");
        assertLexError("let s = \"hola;\nlet t = 1;", "Error léxico [línea 1, col 9]: string sin cerrar");
        assertLexError("a & b", "Error léxico [línea 1, col 3]: carácter ilegal '&' (¿quisiste decir '&&'?)");
        assertLexError("\"a\\q\"", "Error léxico [línea 1, col 1]: secuencia de escape inválida '\\q' en string");
    }

    private void assertLexError(String input, String expectedMessage) {
        Lexer l = new Lexer(input);
        while (l.nextToken().type() != TokenType.EOF) {
            // consumimos todo para que el lexer registre los errores
        }
        assertEquals(expectedMessage, l.errors().get(0));
    }
}
