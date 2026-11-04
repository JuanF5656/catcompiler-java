# Corre todas las pruebas de punta a punta con un solo comando:  .\run_tests.ps1
#  - tests\validos\*.cat y tests\errores\*.cat: compara la salida con nombre.expected
#  - examples\*.cat: compara con el bloque "// === SALIDA ESPERADA ===" del propio archivo
# Solo necesita el JDK (javac y java), no Maven.

Set-Location $PSScriptRoot

Write-Host "Compilando..."
if (Test-Path out) { Remove-Item -Recurse -Force out }
$sources = (Get-ChildItem -Recurse src\main\java -Filter *.java).FullName
javac -encoding UTF-8 -d out $sources
if ($LASTEXITCODE -ne 0) { Write-Host "Falló la compilación"; exit 1 }

# Java escribe en UTF-8 y PowerShell lee en UTF-8: así los acentos coinciden con los .expected
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$script:pass = 0
$script:fail = 0

function Invoke-Cat($file) {
    $lines = & java "-Dstdout.encoding=UTF-8" "-Dsun.stdout.encoding=UTF-8" -cp out catcompiler.Main $file
    return (@($lines) -join "`n").TrimEnd()
}

function Test-Case($name, $expected, $actual) {
    if ($expected -ceq $actual) {
        Write-Host "PASS  $name" -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host "FAIL  $name" -ForegroundColor Red
        Write-Host "      esperado: $($expected -replace "`n", ' | ')"
        Write-Host "      obtenido: $($actual -replace "`n", ' | ')"
        $script:fail++
    }
}

foreach ($f in (Get-ChildItem tests\validos\*.cat, tests\errores\*.cat)) {
    $expectedFile = [System.IO.Path]::ChangeExtension($f.FullName, ".expected")
    $expected = ((Get-Content -Encoding UTF8 $expectedFile) -join "`n").TrimEnd()
    Test-Case "tests\$($f.Directory.Name)\$($f.Name)" $expected (Invoke-Cat $f.FullName)
}

foreach ($f in (Get-ChildItem examples\*.cat)) {
    $lines = Get-Content -Encoding UTF8 $f.FullName
    $start = [Array]::IndexOf($lines, "// === SALIDA ESPERADA ===")
    $expected = (($lines[($start + 1)..($lines.Count - 1)] | ForEach-Object { $_ -replace '^// ?', '' }) -join "`n").TrimEnd()
    Test-Case "examples\$($f.Name)" $expected (Invoke-Cat $f.FullName)
}

Write-Host ""
Write-Host "Resumen: $script:pass pasaron, $script:fail fallaron, $($script:pass + $script:fail) en total"
if ($script:fail -gt 0) { exit 1 }
