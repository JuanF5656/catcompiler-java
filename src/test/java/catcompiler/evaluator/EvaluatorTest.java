package catcompiler.evaluator;

import catcompiler.interpreter.Interpreter;
import catcompiler.object.CatObject;
import catcompiler.object.ErrorObj;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvaluatorTest {

    private static final String EXPECTED_MARKER = "// === SALIDA ESPERADA ===";

    /** Ejecuta el código igual que el modo archivo: lo impreso por print() más el error, si lo hay. */
    private static String run(String source, String input) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        CatObject result = new Interpreter(out, new BufferedReader(new StringReader(input))).run(source);
        if (result instanceof ErrorObj error) {
            out.println(error.message());
        }
        return normalize(buffer.toString(StandardCharsets.UTF_8));
    }

    private static String run(String source) {
        return run(source, "");
    }

    /** Valor de la última expresión, como lo mostraría el REPL. */
    private static String value(String source) {
        CatObject result = new Interpreter(System.out, new BufferedReader(new StringReader(""))).run(source);
        return result.inspect();
    }

    private static String normalize(String text) {
        return text.replace("\r", "").stripTrailing();
    }

    // ============================ PRUEBAS UNITARIAS ============================

    @Test
    void testAritmeticaYPrecedencia() {
        assertEquals("7", value("1 + 2 * 3"));
        assertEquals("512", value("2 ** 3 ** 2"));
        assertEquals("4", value("-2 ** 2"));
        assertEquals("2", value("17 % 5"));
        assertEquals("-3", value("-7 / 2"));
        assertEquals("true", value("true || false && false"));
    }

    @Test
    void testCortocircuitoNoEvaluaElLadoDerecho() {
        // Si se evaluara el lado derecho habría un error de división por cero
        assertEquals("false", value("false && 1 / 0"));
        assertEquals("true", value("true || 1 / 0"));
    }

    @Test
    void testAmbitos() {
        assertEquals("1", value("let x = 1; function f() { let x = 2; } f(); x"));
        assertEquals("2", value("let x = 1; function f() { x = 2; } f(); x"));
        assertEquals("1", value("let x = 1; if (true) { let x = 5; } x"));
    }

    @Test
    void testClosureYRecursion() {
        assertEquals("3", value("function c() { let n = 0; return function() { n = n + 1; return n; }; }"
                + " let k = c(); k(); k(); k()"));
        assertEquals("6765", value("function fib(n) { if (n < 2) { return n; } return fib(n-1) + fib(n-2); } fib(20)"));
        // Recursión profunda: necesita la pila grande del Interpreter
        assertEquals("50005000", value("function s(n) { if (n == 0) { return 0; } return n + s(n - 1); } s(10000)"));
    }

    @Test
    void testReturnYBreakAtraviesanBloques() {
        assertEquals("5", value("function f() { while (true) { if (true) { return 5; } } } f()"));
        assertEquals("3", value("let i = 0; while (true) { i = i + 1; if (i == 3) { break; } } i"));
    }

    @Test
    void testInput() {
        assertEquals("¿Nombre? Hola Limón", run("let n = input(\"¿Nombre? \"); print(\"Hola \" + n);", "Limón\n"));
    }

    @Test
    void testElErrorDetieneLaEjecucion() {
        assertEquals("1\nError de ejecución [línea 1, col 19]: división por cero",
                run("print(1); print(1 / 0); print(2);"));
    }

    // ===================== CASOS DE tests/ Y examples/ =====================

    @TestFactory
    Stream<DynamicTest> testCasosValidosYErrores() throws IOException {
        return Stream.of("tests/validos", "tests/errores")
                .flatMap(EvaluatorTest::catFiles)
                .map(file -> DynamicTest.dynamicTest(file.toString(), () -> {
                    Path expectedFile = Path.of(file.toString().replaceAll("\\.cat$", ".expected"));
                    assertEquals(normalize(Files.readString(expectedFile)), run(Files.readString(file)));
                }));
    }

    @TestFactory
    Stream<DynamicTest> testEjemplos() {
        return catFiles("examples").map(file -> DynamicTest.dynamicTest(file.toString(), () -> {
            String source = Files.readString(file);
            assertEquals(expectedFromComments(source), run(source));
        }));
    }

    /** Lee el bloque "// === SALIDA ESPERADA ===" del final de un ejemplo. */
    private static String expectedFromComments(String source) {
        List<String> lines = source.replace("\r", "").lines().collect(Collectors.toList());
        int start = lines.indexOf(EXPECTED_MARKER);
        return normalize(lines.subList(start + 1, lines.size()).stream()
                .map(line -> line.replaceFirst("^// ?", ""))
                .collect(Collectors.joining("\n")));
    }

    private static Stream<Path> catFiles(String directory) {
        try (Stream<Path> files = Files.list(Path.of(directory))) {
            return files.filter(p -> p.toString().endsWith(".cat")).sorted().collect(Collectors.toList()).stream();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo listar " + directory, e);
        }
    }
}
