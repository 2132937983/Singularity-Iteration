$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$taskOutput = Join-Path $taskRoot 'build/reactor-alignment-audit'
New-Item -ItemType Directory -Force -Path $taskOutput | Out-Null
$taskJavac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { 'javac' }
$taskJava = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
& $taskJavac -encoding UTF-8 -d $taskOutput `
    (Join-Path $taskRoot 'src/main/java/com/miophas/singularity_iteration/core/runtime/reactor/ReactorCycle.java') `
    (Join-Path $taskRoot 'src/main/java/com/miophas/singularity_iteration/core/runtime/reactor/FluidReactorCycle.java') `
    (Join-Path $PSScriptRoot 'ReactorAlignmentProbe.java')
if ($LASTEXITCODE -ne 0) { throw 'Reactor audit compilation failed' }
& $taskJava -cp $taskOutput ReactorAlignmentProbe | Tee-Object -FilePath (Join-Path $taskOutput 'results.txt')
if ($LASTEXITCODE -ne 0) { throw 'Reactor audit execution failed' }
