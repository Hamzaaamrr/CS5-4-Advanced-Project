$ErrorActionPreference = "Continue"

if (!(Test-Path ".\jdk21\jdk-21*")) {
    Write-Host "Extracting OpenJDK 21 using powerful tar..."
    mkdir .\jdk21 -ErrorAction SilentlyContinue
    tar -xf jdk21.zip -C .\jdk21
}
$jdkDir = Get-ChildItem -Path ".\jdk21" -Directory | Select-Object -First 1
$env:JAVA_HOME = $jdkDir.FullName
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH

Write-Host "Java Version loaded:"
java -version

Write-Host "`nStarting Spring Boot Application..."
.\mvnw clean spring-boot:run
