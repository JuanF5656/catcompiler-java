# CatCompiler (Java) 🐾

Port a Java del intérprete **CatCompiler** (original en Go: github.com/sebastianzapatar/compi20262, rama `ast`).
Incluye: **Lexer** + **AST** + **Parser (Pratt)** + **REPL con 2 modos** + pruebas.

## Estructura

```
src/main/java/catcompiler/
├── Main.java               -> banner + menú (1 Lexer / 2 Parser) + REPL
├── token/                  -> Token, TokenType
├── lexer/Lexer.java        -> texto -> tokens
├── ast/                    -> nodos del árbol (Program, LetStatement, InfixExpression, ...)
├── parser/Parser.java      -> tokens -> AST (Pratt parsing)
└── repl/Repl.java          -> consola interactiva
src/test/java/catcompiler/  -> LexerTest, ParserTest (JUnit 5)
```

## Correr desde la terminal (sin Maven, solo JDK)

Windows (PowerShell), parado en la carpeta del proyecto:
```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src\main\java -Filter *.java).FullName
java -cp out catcompiler.Main
```

Mac / Linux:
```bash
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")
java -cp out catcompiler.Main
```

## Con Maven

```bash
mvn compile exec:java   # corre el REPL
mvn test                # corre las pruebas
```

## En VS Code
Abre la carpeta (la que tiene `pom.xml`) con **Extension Pack for Java** y presiona **F5**.

## Ejemplo (modo Parser)

```
Opción [1 o 2]: 2

¡Iniciando en Modo Parser! Escribe código para ver el árbol.
>> 3 + 4 <= 4
AST Generado:
((3 + 4) <= 4)
>> 1 + 2 * 3 ** 2
AST Generado:
(1 + (2 * (3 ** 2)))
>> salir
```
