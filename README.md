# CatCompiler (Java) 🐾

Intérprete del lenguaje **CatCompiler** escrito en Java 17: un port del original en Go
(github.com/sebastianzapatar/compi20262), extendido hasta tener un evaluador completo.
Es el trabajo final de Compiladores de la Universidad EIA.

```
texto fuente → Lexer → tokens → Parser (Pratt) → AST → Evaluator → resultado
```

Incluye variables, expresiones aritméticas, relacionales y lógicas con precedencia,
`if/elif/else`, `while`, `for` y `break`, funciones con recursión, ámbitos con sombreado,
listas, diccionarios, 11 funciones integradas y errores léxicos, sintácticos y de ejecución
con línea y columna. Como extensiones trae closures, cortocircuito y funciones de orden superior.

## Requisitos

- **JDK 17 o superior** (`java -version`)
- **Maven 3.8+**, solo si se quiere usar `mvn`; los scripts de prueba no lo necesitan
- Sin dependencias externas aparte de JUnit 5 (solo para las pruebas)

## Estructura

```
src/main/java/catcompiler/
├── Main.java                  -> menú del REPL o ejecución de un archivo
├── token/                     -> Token (tipo, literal, línea, columna), TokenType
├── lexer/Lexer.java           -> texto -> tokens (y errores léxicos)
├── ast/                       -> nodos del árbol (Program, LetStatement, InfixExpression, ...)
├── parser/Parser.java         -> tokens -> AST (Pratt parsing, errores sintácticos)
├── object/                    -> valores en ejecución (IntegerObj, ArrayObj, FunctionObj, ...) y Environment
├── evaluator/                 -> Evaluator (recorre el AST), Operators, Builtins
├── interpreter/Interpreter.java -> une las etapas; lo usan el REPL, el modo archivo y los tests
└── repl/Repl.java             -> consola interactiva (modos Lexer, Parser y Evaluador)
src/test/java/catcompiler/     -> LexerTest, ParserTest, EvaluatorTest (JUnit 5)
examples/                      -> un programa .cat por característica, con su salida esperada
tests/validos, tests/errores   -> casos nombre.cat + nombre.expected
docs/DISENO.md                 -> gramática EBNF, arquitectura y decisiones de diseño
docs/GUIA_SUSTENTACION.md      -> recorrido completo, ejercicios resueltos y preguntas de examen
```

## Compilar

Con solo el JDK (crea la carpeta `out/`):

```powershell
# Windows (PowerShell)
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src\main\java -Filter *.java).FullName
```
```bash
# Mac / Linux / Git Bash
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")
```

Con Maven: `mvn compile`

## Ejecutar

**REPL** (menú con 3 modos: 1 Lexer, 2 Parser, 3 Evaluador):

```bash
java -cp out catcompiler.Main
mvn compile exec:java
```

En modo Evaluador las variables se conservan entre líneas. Si una línea deja `{`, `(` o `[`
abiertos, el REPL sigue leyendo (prompt `..`) hasta cerrarlos. Un error muestra su mensaje
y el REPL continúa. Para salir se escribe `salir` o `exit`.

```
>> let x = 5;
>> function cuadrado(n) {
..   return n * n;
.. }
function(n)
>> cuadrado(x)
25
>> 1 / 0
Error de ejecución [línea 1, col 3]: división por cero
```

**Desde un archivo**:

```bash
java -cp out catcompiler.Main examples/05_funciones_recursion.cat
mvn compile exec:java -Dexec.args="examples/01_variables.cat"
```

Si hay un error, se imprime el mensaje y el proceso termina con código 1.

> En Windows, si los acentos se ven mal en la consola, ejecuta `chcp 65001` antes
> o agrega `-Dstdout.encoding=UTF-8` al comando `java`.

## Correr las pruebas

```bash
mvn clean test          # JUnit: lexer, parser, evaluador + todos los tests/ y examples/
./run_tests.sh          # Bash: compila con javac y compara cada salida (PASS/FAIL + resumen)
.\run_tests.ps1         # PowerShell: lo mismo en Windows
```

En PowerShell, si los scripts están bloqueados:
`powershell -ExecutionPolicy Bypass -File .\run_tests.ps1`

Los scripts revisan `tests/validos`, `tests/errores` (cada `nombre.cat` contra
`nombre.expected`) y `examples/` (contra el bloque `// === SALIDA ESPERADA ===` al final de
cada ejemplo).

## El lenguaje

El `;` al final de una sentencia es opcional. Los comentarios van con `//` hasta el final de la línea.

### Tipos de datos

| Tipo | Ejemplo | `type()` |
|---|---|---|
| entero (64 bits) | `42`, `-7` | `"entero"` |
| string | `"hola\n"` (escapes `\n \t \" \\`) | `"string"` |
| booleano | `true`, `false` | `"booleano"` |
| lista | `[1, "dos", [3]]` | `"lista"` |
| diccionario | `{"a": 1, 2: true}` | `"diccionario"` |
| función | `function(x) { x }` | `"funcion"` |
| nulo | lo que devuelven `let`, `print`, un `if` sin `else`... | `"nulo"` |

Solo `false` y el nulo son falsos; todo lo demás (incluido `0` y `""`) es verdadero.

### Variables y asignación

```
let x = 10;        // declara en el ámbito actual
x = x + 1;         // reasigna la variable existente más cercana (error si no existe)
let lista = [1, 2, 3];
lista[0] = 99;     // asignación por índice (también en diccionarios)
```

### Operadores (de menor a mayor precedencia)

| Nivel | Operadores | Asociatividad |
|---|---|---|
| 1 | `=` | derecha |
| 2 | `\|\|` | izquierda (con cortocircuito) |
| 3 | `&&` | izquierda (con cortocircuito) |
| 4 | `==` `!=` | izquierda |
| 5 | `<` `>` `<=` `>=` | izquierda |
| 6 | `+` `-` | izquierda |
| 7 | `*` `/` `%` | izquierda |
| 8 | `**` | derecha |
| 9 | `-x` `!x` (prefijos) | — |
| 10 | `f(x)` `a[i]` | izquierda |

`+` también concatena strings. `/` es división entera. Como los prefijos están por encima
de `**`, `-2 ** 2` vale `4`.

### Condicionales

```
if (n < 0) {
    print("negativo");
} elif (n == 0) {
    print("cero");
} else {
    print("positivo");
}
let paridad = if (n % 2 == 0) { "par" } else { "impar" };   // if es una expresión
```

### Ciclos

```
while (i < 10) { i = i + 1; }
for (i < 10) { i = i + 1; }                         // igual que while
for (let i = 0; i < 10; i = i + 1) { print(i); }    // estilo C: i solo existe dentro
while (true) { if (listo) { break; } }              // break sale del ciclo más cercano
```

### Funciones

```
let suma = function(a, b) { return a + b; };        // anónima
function factorial(n) {                             // con nombre
    if (n <= 1) { return 1; }
    return n * factorial(n - 1);
}
function crearContador() {                          // closure
    let n = 0;
    return function() { n = n + 1; return n; };
}
```

Sin `return`, una función devuelve el valor de su última expresión.

### Funciones integradas

| Función | Qué hace |
|---|---|
| `print(a, b, ...)` | imprime los valores separados por espacio |
| `input()` / `input("msg")` | lee una línea de la entrada estándar |
| `len(x)` | largo de un string, una lista o un diccionario |
| `str(x)` | convierte a string |
| `int(x)` | convierte un string, booleano o entero a entero |
| `type(x)` | nombre del tipo |
| `push(lista, v)` | agrega al final (modifica la lista) y la devuelve |
| `first(l)` / `last(l)` | primer o último elemento |
| `rest(l)` | lista nueva sin el primer elemento |
| `keys(d)` | lista con las claves de un diccionario |

### Errores

Todos indican el tipo de error, la línea y la columna, y detienen la ejecución:

```
Error léxico [línea 3, col 20]: carácter ilegal '$'
Error sintáctico [línea 2, col 15]: se esperaba ')' pero se encontró ';'
Error de ejecución [línea 4, col 12]: índice fuera de rango: 3 (tamaño 3)
```

## Integrantes

- Juan Felipe Atehortua
- Thomas González 
