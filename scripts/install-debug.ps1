$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "resolve-android-sdk.ps1")

& (Join-Path $PSScriptRoot "build-debug.ps1")

$sdkRoot = Resolve-AndroidSdkRoot -RepoRoot $repoRoot

$adb = Join-Path $sdkRoot "platform-tools\adb.exe"
$apk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path -LiteralPath $adb)) {
    throw "未找到 adb：$adb"
}

if (-not (Test-Path -LiteralPath $apk)) {
    throw "未找到 APK：$apk"
}

& $adb install -r $apk
