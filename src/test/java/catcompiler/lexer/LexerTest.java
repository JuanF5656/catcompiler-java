package catcompiler.lexer;

import catcompiler.token.Token;
import catcompiler.token.TokenType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
