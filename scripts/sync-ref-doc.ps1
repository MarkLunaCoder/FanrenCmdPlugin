param(
    [string]$UpstreamUrl = "https://github.com/iwooji77/fanren.git",
    [string]$WorkDir = (Join-Path ([System.IO.Path]::GetTempPath()) "fanren-upstream"),
    [switch]$FullDiff,
    [switch]$Apply
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

$repoRoot = Split-Path -Parent $PSScriptRoot
$refDocRoot = Join-Path $repoRoot "ref_doc\fanren-introduce"
$files = @(
    "README.md",
    "xiuxian_game_commands.md",
    "xiuxian_game_guide.md"
)

function Normalize-GitRemoteUrl {
    param([string]$Url)

    if ([string]::IsNullOrWhiteSpace($Url)) {
        return ""
    }

    $normalized = $Url.Trim().TrimEnd("/")
    if ($normalized.EndsWith(".git", [System.StringComparison]::OrdinalIgnoreCase)) {
        $normalized = $normalized.Substring(0, $normalized.Length - 4)
    }
    return $normalized.ToLowerInvariant()
}

if (-not (Get-Command "git.exe" -ErrorAction SilentlyContinue)) {
    throw "未找到 git。请先安装 Git，或把 git 加入 PATH。"
}

if (Test-Path -LiteralPath (Join-Path $WorkDir ".git")) {
    $actualUrl = (& git -C $WorkDir remote get-url origin 2>$null)
    $actualUrlText = ($actualUrl -join "").Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($actualUrlText)) {
        throw "同步目录缺少 origin remote：$WorkDir"
    }
    if ((Normalize-GitRemoteUrl $actualUrlText) -ne (Normalize-GitRemoteUrl $UpstreamUrl)) {
        throw "同步目录的 origin remote 与目标上游不一致：$actualUrlText"
    }
    git -C $WorkDir pull --ff-only
    if ($LASTEXITCODE -ne 0) {
        throw "更新上游仓库失败：$WorkDir"
    }
} else {
    if (Test-Path -LiteralPath $WorkDir) {
        throw "同步目录已存在但不是 Git 仓库：$WorkDir"
    }
    git clone $UpstreamUrl $WorkDir
    if ($LASTEXITCODE -ne 0) {
        throw "克隆上游仓库失败：$UpstreamUrl"
    }
}

if ($Apply) {
    $localChanges = (& git -C $repoRoot status --porcelain -- "ref_doc/fanren-introduce")
    $localChangesText = ($localChanges -join [Environment]::NewLine)
    if (-not [string]::IsNullOrWhiteSpace($localChangesText)) {
        throw "本地 ref_doc/fanren-introduce 存在未提交改动，请先提交或放弃这些改动后再使用 -Apply。"
    }
    Write-Host "注意：-Apply 会用上游原始文件覆盖本地 ref_doc，覆盖后仍需人工校验解析格式、命令分类和本地化调整。" -ForegroundColor Yellow
}

$upstreamCommit = (git -C $WorkDir rev-parse HEAD).Trim()
Write-Host "上游提交：$upstreamCommit"
Write-Host "本地资料：$refDocRoot"

foreach ($file in $files) {
    $localFile = Join-Path $refDocRoot $file
    $upstreamFile = Join-Path $WorkDir $file
    if (-not (Test-Path -LiteralPath $upstreamFile)) {
        Write-Host "跳过，上游不存在：$file" -ForegroundColor Yellow
        continue
    }

    if ($Apply) {
        Copy-Item -LiteralPath $upstreamFile -Destination $localFile -Force
        Write-Host "已同步：$file"
        continue
    }

    Write-Host ""
    Write-Host "对比：$file"
    if (-not (Test-Path -LiteralPath $localFile)) {
        Write-Host "跳过，本地不存在：$file" -ForegroundColor Yellow
        continue
    }

    if ($FullDiff) {
        git diff --no-index -- $localFile $upstreamFile
    } else {
        git diff --no-index --stat -- $localFile $upstreamFile
    }
    $diffExitCode = $LASTEXITCODE
    if ($diffExitCode -eq 0) {
        Write-Host "无差异：$file"
    } elseif ($diffExitCode -gt 1) {
        throw "对比失败：$file"
    }
}

if (-not $Apply) {
    Write-Host ""
    Write-Host "默认只显示差异摘要，不改本地文件。需要完整差异时运行："
    Write-Host ".\scripts\sync-ref-doc.ps1 -FullDiff"
    Write-Host "确认要覆盖本地 ref_doc 时，再运行："
    Write-Host ".\scripts\sync-ref-doc.ps1 -Apply"
}
