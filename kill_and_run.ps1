$ErrorActionPreference = "Continue"

# Always work from the project directory (where this script lives)
$projectDir = $PSScriptRoot
Set-Location $projectDir

# Kill anything on port 8080
$lines = netstat -ano | Select-String ":8080"
foreach ($line in $lines) {
    $parts = $line.ToString().Trim() -split "\s+"
    $pid_val = $parts[-1]
    if ($pid_val -match "^\d+$" -and [int]$pid_val -ne 0) {
        Write-Host "Killing PID $pid_val on port 8080"
        Stop-Process -Id ([int]$pid_val) -Force -ErrorAction SilentlyContinue
    }
}

Start-Sleep -Seconds 2

# Set JAVA_HOME to bundled JDK 21 (absolute path)
$jdkBase = Join-Path $projectDir "jdk21"
$jdkDir = Get-ChildItem -Path $jdkBase -Directory | Select-Object -First 1
$env:JAVA_HOME = $jdkDir.FullName
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH

Write-Host "Using Java:"
java -version

Write-Host "`nStarting Spring Boot Application on http://localhost:8080 ..."
$mvnw = Join-Path $projectDir "mvnw.cmd"
& $mvnw clean spring-boot:run
