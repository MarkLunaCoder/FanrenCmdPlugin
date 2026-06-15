$ErrorActionPreference = "Stop"

function Convert-LocalPropertiesPath {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ""
    }
    return $Value.Trim().Replace("\:", ":").Replace("\\", "\")
}

function Get-LocalPropertiesValue {
    param(
        [string]$RepoRoot,
        [string]$Name
    )

    $localProperties = Join-Path $RepoRoot "local.properties"
    if (-not (Test-Path -LiteralPath $localProperties)) {
        return ""
    }

    $escapedName = [regex]::Escape($Name)
    foreach ($line in Get-Content -LiteralPath $localProperties -Encoding UTF8) {
        if ($line -match "^\s*$escapedName\s*=\s*(.+)\s*$") {
            return Convert-LocalPropertiesPath -Value $matches[1]
        }
    }
    return ""
}

function Get-AndroidSdkFromLocalProperties {
    param([string]$RepoRoot)

    return Get-LocalPropertiesValue -RepoRoot $RepoRoot -Name "sdk.dir"
}

function Resolve-AndroidSdkRoot {
    param([string]$RepoRoot)

    $candidates = @(
        $env:ANDROID_HOME,
        $env:ANDROID_SDK_ROOT,
        (Get-AndroidSdkFromLocalProperties -RepoRoot $RepoRoot)
    )

    foreach ($candidate in $candidates) {
        if ([string]::IsNullOrWhiteSpace($candidate)) {
            continue
        }
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    throw "未找到 Android SDK。请设置 ANDROID_HOME 或 ANDROID_SDK_ROOT，或在 local.properties 中配置 sdk.dir。"
}
