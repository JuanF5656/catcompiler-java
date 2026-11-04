package catcompiler.evaluator;

import catcompiler.ast.*;
import catcompiler.object.*;
import catcompiler.token.Token;

import java.util.List;

import static catcompiler.object.NullObj.NULL;

/**
 * Evaluador de árbol (tree-walking interpreter): recorre el AST y produce
 * valores (CatObject).
 *
 * Patrón de errores: un error de ejecución es un ErrorObj que se DEVUELVE
 * (no se lanza). Después de evaluar cualquier subexpresión se revisa
 * isError(...) y, si lo es, se devuelve tal cual; así el primer error sube
 * hasta el programa y detiene todo. ReturnValue y BreakSignal suben igual,
 * hasta que los atrapa la llamada a función o el ciclo, respectivamente.
 */
public class Evaluator {
    private final Builtins builtins;

    public Evaluator(Builtins builtins) {
        this.builtins = builtins;
    }

    /** Punto de entrada: despacha según el tipo de nodo. */
    public CatObject eval(Node node, Environment env) {
        // Sentencias
        if (node instanceof Program p) return evalProgram(p, env);
        if (node instanceof ExpressionStatement s) return eval(s.expression, env);
        if (node instanceof LetStatement s) return evalLet(s, env);
        if (node instanceof ReturnStatement s) return evalReturn(s, env);
        if (node instanceof BreakStatement s) return new BreakSignal(s.token);
        // Cada bloque { } abre su propio ámbito: un let adentro no se ve afuera
        if (node instanceof BlockStatement b) return evalStatements(b.statements, new Environment(env));

        // Literales
        if (node instanceof IntegerLiteral l) return new IntegerObj(l.value);
        if (node instanceof StringLiteral l) return new StringObj(l.value);
        if (node instanceof BooleanLiteral l) return BooleanObj.of(l.value);
        if (node instanceof ArrayLiteral a) return evalExpressions(a.elements, env);
        if (node instanceof HashLiteral h) return evalHash(h, env);
        if (node instanceof FunctionLiteral f) return evalFunctionLiteral(f, env);

        // Expresiones
        if (node instanceof Identifier i) return evalIdentifier(i, env);
        if (node instanceof PrefixExpression e) return evalPrefix(e, env);
        if (node instanceof InfixExpression e) return evalInfix(e, env);
        if (node instanceof AssignExpression e) return evalAssign(e, env);
        if (node instanceof IndexExpression e) return evalIndex(e, env);
        if (node instanceof CallExpression c) return evalCall(c, env);

        // Control de flujo
        if (node instanceof IfExpression e) return evalIf(e, env);
        if (node instanceof WhileExpression w) return runLoop(w.condition, w.body, null, env);
        if (node instanceof ForExpression f) return evalFor(f, env);

        throw new IllegalStateException("Nodo sin evaluar: " + node.getClass().getSimpleName());
    }

    // ============================= SENTENCIAS =============================

    private CatObject evalProgram(Program program, Environment env) {
        return unwrap(evalStatements(program.statements, env));
    }

    /**
     * Ejecuta en orden y se detiene ante return, break o error SIN abrirlos:
     * el que sabe qué hacer con ellos está más arriba (función, ciclo, programa).
     */
    private CatObject evalStatements(List<Statement> statements, Environment env) {
        CatObject result = NULL;
        for (Statement statement : statements) {
            result = eval(statement, env);
            if (result instanceof ReturnValue || result instanceof BreakSignal || isError(result)) {
                return result;
            }
        }
        return result;
    }

    /**
     * Al terminar una función o el programa: aquí se abre el return, y un
     * break que llegó hasta aquí es porque no estaba dentro de ningún ciclo.
     */
    private static CatObject unwrap(CatObject result) {
        if (result instanceof ReturnValue r) {
            return r.value();
        }
        if (result instanceof BreakSignal b) {
            return ErrorObj.at(b.token(), "break fuera de un ciclo");
        }
        return result;
    }

    private CatObject evalLet(LetStatement let, Environment env) {
        CatObject value = eval(let.value, env);
        if (isError(value)) {
            return value;
        }
        env.define(let.name.value, value);
        return NULL;
    }

    private CatObject evalReturn(ReturnStatement ret, Environment env) {
        if (ret.returnValue == null) {
            return new ReturnValue(NULL);
        }
        CatObject value = eval(ret.returnValue, env);
        return isError(value) ? value : new ReturnValue(value);
    }

    // ============================ EXPRESIONES ============================

    private CatObject evalIdentifier(Identifier identifier, Environment env) {
        CatObject value = env.get(identifier.value);
        if (value != null) {
            return value;
        }
        // Las integradas se buscan al final: así el usuario puede sombrearlas con let
        BuiltinObj builtin = builtins.get(identifier.value);
        if (builtin != null) {
            return builtin;
        }
        return ErrorObj.at(identifier.token, "variable no definida '" + identifier.value + "'");
    }

    private CatObject evalPrefix(PrefixExpression expression, Environment env) {
        CatObject right = eval(expression.right, env);
        if (isError(right)) {
            return right;
        }
        return Operators.prefix(expression.token, expression.operator, right);
    }

    private CatObject evalInfix(InfixExpression expression, Environment env) {
        if (expression.operator.equals("&&") || expression.operator.equals("||")) {
            return evalLogical(expression, env);
        }
        CatObject left = eval(expression.left, env);
        if (isError(left)) {
            return left;
        }
        CatObject right = eval(expression.right, env);
        if (isError(right)) {
            return right;
        }
        return Operators.infix(expression.token, expression.operator, left, right);
    }

    /**
     * Cortocircuito: si el lado izquierdo ya decide el resultado, el derecho
     * NO se evalúa (false && x, true || x). Por eso && y || no pueden ir en
     * Operators: allá los dos lados ya llegan evaluados.
     */
    private CatObject evalLogical(InfixExpression expression, Environment env) {
        CatObject left = eval(expression.left, env);
        if (isError(left)) {
            return left;
        }
        boolean leftIsTrue = Operators.isTruthy(left);
        boolean isAnd = expression.operator.equals("&&");
        if (isAnd != leftIsTrue) {
            return BooleanObj.of(leftIsTrue); // && con izquierda falsa, o || con izquierda verdadera
        }
        CatObject right = eval(expression.right, env);
        return isError(right) ? right : BooleanObj.of(Operators.isTruthy(right));
    }

    private CatObject evalAssign(AssignExpression assign, Environment env) {
        CatObject value = eval(assign.value, env);
        if (isError(value)) {
            return value;
        }
        if (assign.target instanceof IndexExpression target) {
            return assignIndex(target, value, env);
        }
        Identifier name = (Identifier) assign.target; // el parser garantiza que es uno de los dos
        if (!env.assign(name.value, value)) {
            return ErrorObj.at(name.token, "variable no definida '" + name.value + "' (declárala primero con let)");
        }
        return value;
    }

    /** Evalúa una lista de expresiones en orden; se usa para listas literales y argumentos. */
    private CatObject evalExpressions(List<Expression> expressions, Environment env) {
        ArrayObj values = new ArrayObj(List.of());
        for (Expression expression : expressions) {
            CatObject value = eval(expression, env);
            if (isError(value)) {
                return value;
            }
            values.elements.add(value);
        }
        return values;
    }

    private CatObject evalHash(HashLiteral literal, Environment env) {
        HashObj hash = new HashObj();
        for (HashLiteral.Pair pair : literal.pairs) {
            CatObject key = eval(pair.key(), env);
            if (isError(key)) {
                return key;
            }
            if (!(key instanceof Hashable hashableKey)) {
                return unhashable(literal.token, key);
            }
            CatObject value = eval(pair.value(), env);
            if (isError(value)) {
                return value;
            }
            hash.pairs.put(hashableKey, value);
        }
        return hash;
    }

    // ========================= ÍNDICES  lista[i] =========================

    private CatObject evalIndex(IndexExpression expression, Environment env) {
        CatObject left = eval(expression.left, env);
        if (isError(left)) {
            return left;
        }
        CatObject index = eval(expression.index, env);
        if (isError(index)) {
            return index;
        }
        Token token = expression.token;
        if (left instanceof HashObj hash) {
            return hashGet(token, hash, index);
        }
        if (left instanceof StringObj text) {
            ErrorObj error = checkPosition(token, index, text.value().length());
            return error != null ? error : new StringObj(String.valueOf(text.value().charAt(position(index))));
        }
        if (left instanceof ArrayObj array) {
            ErrorObj error = checkPosition(token, index, array.elements.size());
            return error != null ? error : array.elements.get(position(index));
        }
        return ErrorObj.at(token, "no se puede indexar un valor de tipo " + left.type());
    }

    private CatObject assignIndex(IndexExpression target, CatObject value, Environment env) {
        CatObject container = eval(target.left, env);
        if (isError(container)) {
            return container;
        }
        CatObject index = eval(target.index, env);
        if (isError(index)) {
            return index;
        }
        if (container instanceof HashObj hash) {
            if (!(index instanceof Hashable key)) {
                return unhashable(target.token, index);
            }
            hash.pairs.put(key, value);
            return value;
        }
        if (container instanceof ArrayObj array) {
            ErrorObj error = checkPosition(target.token, index, array.elements.size());
            if (error != null) {
                return error;
            }
            array.elements.set(position(index), value);
            return value;
        }
        return ErrorObj.at(target.token, "no se puede asignar por índice a un valor de tipo " + container.type());
    }

    private static CatObject hashGet(Token token, HashObj hash, CatObject key) {
        if (!(key instanceof Hashable hashableKey)) {
            return unhashable(token, key);
        }
        CatObject value = hash.pairs.get(hashableKey);
        return value != null ? value : ErrorObj.at(token, "clave no encontrada: " + key.inspect());
    }

    /** Valida que el índice sea un entero dentro de [0, size). Devuelve null si es válido. */
    private static ErrorObj checkPosition(Token token, CatObject index, int size) {
        if (!(index instanceof IntegerObj i)) {
            return ErrorObj.at(token, "el índice debe ser un entero, no " + index.type());
        }
        if (i.value() < 0 || i.value() >= size) {
            return ErrorObj.at(token, "índice fuera de rango: " + i.value() + " (tamaño " + size + ")");
        }
        return null;
    }

    private static int position(CatObject validIndex) {
        return (int) ((IntegerObj) validIndex).value();
    }

    private static ErrorObj unhashable(Token token, CatObject key) {
        return ErrorObj.at(token, "clave no hashable: " + key.type() + " (solo entero, string o booleano)");
    }

    // ============================== FUNCIONES ==============================

    /**
     * La función captura `env`, el entorno donde se está DEFINIENDO. Ese es
     * todo el truco de los closures. Si tiene nombre, también se declara en
     * ese mismo entorno, por eso puede llamarse a sí misma (recursión).
     */
    private CatObject evalFunctionLiteral(FunctionLiteral literal, Environment env) {
        FunctionObj function = new FunctionObj(literal.parameters, literal.body, env);
        if (literal.name != null) {
            env.define(literal.name.value, function);
        }
        return function;
    }

    private CatObject evalCall(CallExpression call, Environment env) {
        CatObject function = eval(call.function, env);
        if (isError(function)) {
            return function;
        }
        CatObject args = evalExpressions(call.arguments, env);
        if (isError(args)) {
            return args;
        }
        return applyFunction(call.token, function, ((ArrayObj) args).elements);
    }

    private CatObject applyFunction(Token callToken, CatObject function, List<CatObject> args) {
        if (function instanceof BuiltinObj builtin) {
            return builtin.function().apply(callToken, args);
        }
        if (!(function instanceof FunctionObj fn)) {
            return ErrorObj.at(callToken, "no se puede llamar a un valor de tipo " + function.type() + ", no es una función");
        }
        if (args.size() != fn.parameters().size()) {
            return ErrorObj.at(callToken, "la función esperaba " + Builtins.count(fn.parameters().size())
                    + " pero recibió " + args.size());
        }
        // outer = entorno de la DEFINICIÓN (fn.env), no el de quien llama:
        // eso es alcance léxico y es lo que hace funcionar los closures.
        Environment callEnv = new Environment(fn.env());
        for (int i = 0; i < args.size(); i++) {
            callEnv.define(fn.parameters().get(i).value, args.get(i));
        }
        return unwrap(evalStatements(fn.body().statements, callEnv));
    }

    // =========================== CONTROL DE FLUJO ===========================

    private CatObject evalIf(IfExpression expression, Environment env) {
        CatObject condition = eval(expression.condition, env);
        if (isError(condition)) {
            return condition;
        }
        if (Operators.isTruthy(condition)) {
            return eval(expression.consequence, env);
        }
        return expression.alternative != null ? eval(expression.alternative, env) : NULL;
    }

    /** El for tiene su propio ámbito para que la variable del init no se escape del ciclo. */
    private CatObject evalFor(ForExpression loop, Environment env) {
        Environment loopEnv = new Environment(env);
        if (loop.init != null) {
            CatObject init = eval(loop.init, loopEnv);
            if (isError(init)) {
                return init;
            }
        }
        return runLoop(loop.condition, loop.body, loop.update, loopEnv);
    }

    /**
     * Ciclo común de while y for: condición -> cuerpo -> actualización.
     * break termina el ciclo; return y error se dejan subir sin tocarlos.
     */
    private CatObject runLoop(Expression condition, BlockStatement body, Expression update, Environment env) {
        while (true) {
            CatObject test = eval(condition, env);
            if (isError(test)) {
                return test;
            }
            if (!Operators.isTruthy(test)) {
                return NULL;
            }
            CatObject result = eval(body, env);
            if (result instanceof BreakSignal) {
                return NULL;
            }
            if (result instanceof ReturnValue || isError(result)) {
                return result;
            }
            if (update != null) {
                CatObject updated = eval(update, env);
                if (isError(updated)) {
                    return updated;
                }
            }
        }
    }

    private static boolean isError(CatObject value) {
        return value instanceof ErrorObj;
    }
}
