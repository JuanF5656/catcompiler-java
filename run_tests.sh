#!/usr/bin/env bash
# Corre todas las pruebas de punta a punta con un solo comando:  ./run_tests.sh
#  - tests/validos/*.cat y tests/errores/*.cat: compara la salida con nombre.expected
#  - examples/*.cat: compara con el bloque "// === SALIDA ESPERADA ===" del propio archivo
# Solo necesita el JDK (javac y java), no Maven.

cd "$(dirname "$0")" || exit 1

echo "Compilando..."
rm -rf out
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java") || { echo "Falló la compilación"; exit 1; }

# Forzamos UTF-8 en la salida para que los acentos coincidan con los .expected
JAVA="java -Dstdout.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -cp out catcompiler.Main"
pass=0
fail=0

# check <nombre> <salida esperada> <salida real>
check() {
    if [ "$2" == "$3" ]; then
        echo "PASS  $1"
        pass=$((pass + 1))
    else
        echo "FAIL  $1"
        diff <(echo "$2") <(echo "$3") | sed 's/^/      /'
        fail=$((fail + 1))
    fi
}

for f in tests/validos/*.cat tests/errores/*.cat; do
    expected=$(tr -d '\r' < "${f%.cat}.expected")
    actual=$($JAVA "$f" | tr -d '\r')
    check "$f" "$expected" "$actual"
done

for f in examples/*.cat; do
    expected=$(sed -n '/^\/\/ === SALIDA ESPERADA ===/,$p' "$f" | tail -n +2 | tr -d '\r' | sed -e 's|^// \{0,1\}||')
    actual=$($JAVA "$f" | tr -d '\r')
    check "$f" "$expected" "$actual"
done

echo
echo "Resumen: $pass pasaron, $fail fallaron, $((pass + fail)) en total"
[ "$fail" -eq 0 ]
