$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "resolve-android-sdk.ps1")

$jdkRoot = $env:JAVA_HOME
$javaExe = if ([string]::IsNullOrWhiteSpace($jdkRoot)) { "" } else { [System.IO.Path]::Combine($jdkRoot, "bin", "java.exe") }
if ([string]::IsNullOrWhiteSpace($jdkRoot) -or -not (Test-Path -LiteralPath $javaExe)) {
    $localJavaHome = Get-LocalPropertiesValue -RepoRoot $repoRoot -Name "java.home"
    $localJavaExe = if ([string]::IsNullOrWhiteSpace($localJavaHome)) { "" } else { [System.IO.Path]::Combine($localJavaHome, "bin", "java.exe") }
    if (-not [string]::IsNullOrWhiteSpace($localJavaHome) -and (Test-Path -LiteralPath $localJavaExe)) {
        $env:JAVA_HOME = $localJavaHome
        $env:Path = (Join-Path $localJavaHome "bin") + ";" + $env:Path
    } else {
        $javaCommand = Get-Command "java.exe" -ErrorAction SilentlyContinue
        if ($null -eq $javaCommand) {
            throw "未找到 Java。请安装 JDK 17 或更高版本，并设置 JAVA_HOME、local.properties 的 java.home，或把 java 加入 PATH。"
        }
    }
} else {
    $env:JAVA_HOME = $jdkRoot
    $env:Path = (Join-Path $jdkRoot "bin") + ";" + $env:Path
}

$sdkRoot = Resolve-AndroidSdkRoot -RepoRoot $repoRoot
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
Set-Location $repoRoot

.\gradlew.bat :app:assembleDebug
