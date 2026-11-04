package catcompiler.evaluator;

import catcompiler.object.BooleanObj;
import catcompiler.object.CatObject;
import catcompiler.object.ErrorObj;
import catcompiler.object.IntegerObj;
import catcompiler.object.NullObj;
import catcompiler.object.StringObj;
import catcompiler.token.Token;

/**
 * Operadores prefijos e infijos sobre valores ya evaluados. Está separado del
 * Evaluator porque aquí no hay AST ni entornos: solo "valor op valor -> valor",
 * lo que lo hace fácil de leer y de probar.
 * (&& y || no están aquí: necesitan el AST para hacer cortocircuito.)
 */
final class Operators {

    private Operators() {
    }

    /** Solo false y null son "falsos"; todo lo demás (incluido 0) cuenta como verdadero. */
    static boolean isTruthy(CatObject value) {
        if (value instanceof BooleanObj b) {
            return b.value;
        }
        return value != NullObj.NULL;
    }

    static CatObject prefix(Token token, String operator, CatObject right) {
        if (operator.equals("!")) {
            return BooleanObj.of(!isTruthy(right));
        }
        if (right instanceof IntegerObj i) { // operator es "-"
            return new IntegerObj(-i.value());
        }
        return ErrorObj.at(token, "el operador '-' no se puede aplicar a un valor de tipo " + right.type());
    }

    static CatObject infix(Token token, String operator, CatObject left, CatObject right) {
        if (left instanceof IntegerObj l && right instanceof IntegerObj r) {
            return integerInfix(token, operator, l.value(), r.value());
        }
        if (left instanceof StringObj l && right instanceof StringObj r) {
            return stringInfix(token, operator, l.value(), r.value());
        }
        // Para el resto (booleanos, null, listas...) solo hay igualdad.
        // Valores de tipos distintos nunca son iguales, pero compararlos no es error.
        if (operator.equals("==")) {
            return BooleanObj.of(left.equals(right));
        }
        if (operator.equals("!=")) {
            return BooleanObj.of(!left.equals(right));
        }
        if (!left.type().equals(right.type())) {
            return ErrorObj.at(token, "tipos incompatibles: " + left.type() + " " + operator + " " + right.type());
        }
        return unsupported(token, operator, left.type());
    }

    private static CatObject integerInfix(Token token, String operator, long l, long r) {
        return switch (operator) {
            case "+" -> new IntegerObj(l + r);
            case "-" -> new IntegerObj(l - r);
            case "*" -> new IntegerObj(l * r);
            case "/" -> r == 0 ? divisionByZero(token, "división") : new IntegerObj(l / r);
            case "%" -> r == 0 ? divisionByZero(token, "módulo") : new IntegerObj(l % r);
            case "**" -> power(token, l, r);
            case "<" -> BooleanObj.of(l < r);
            case ">" -> BooleanObj.of(l > r);
            case "<=" -> BooleanObj.of(l <= r);
            case ">=" -> BooleanObj.of(l >= r);
            case "==" -> BooleanObj.of(l == r);
            case "!=" -> BooleanObj.of(l != r);
            default -> unsupported(token, operator, "entero");
        };
    }

    private static CatObject stringInfix(Token token, String operator, String l, String r) {
        return switch (operator) {
            case "+" -> new StringObj(l + r);
            case "==" -> BooleanObj.of(l.equals(r));
            case "!=" -> BooleanObj.of(!l.equals(r));
            default -> unsupported(token, operator, "string");
        };
    }

    /** Potencia entera por multiplicaciones: Math.pow usa double y perdería precisión. */
    private static CatObject power(Token token, long base, long exponent) {
        if (exponent < 0) {
            return ErrorObj.at(token, "exponente negativo en '**' (el lenguaje solo tiene enteros)");
        }
        long result = 1;
        for (long i = 0; i < exponent; i++) {
            result *= base;
        }
        return new IntegerObj(result);
    }

    private static ErrorObj divisionByZero(Token token, String operation) {
        return ErrorObj.at(token, operation + " por cero");
    }

    private static ErrorObj unsupported(Token token, String operator, String type) {
        return ErrorObj.at(token, "el operador '" + operator + "' no se puede aplicar a valores de tipo " + type);
    }
}
