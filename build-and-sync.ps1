# ============================================================
# build-and-sync.ps1
# 一键构建 pvp-optimize, 成功后自动同步到 PCL 的 mods 文件夹.
# ============================================================

$ErrorActionPreference = "Stop"

$projectDir = "C:\Users\16210\Documents\pvp-optimize"
$modsDir = "C:\Users\16210\Desktop\PCL 正式版 2.13.0.1\.minecraft\versions\Simply Optimized & Up to Date\mods"
$jarName = "pvp-optimize-1.0.2.jar"
$src = Join-Path $projectDir "build\libs\$jarName"
$dst = Join-Path $modsDir $jarName
$backupDir = Join-Path $modsDir "pvp-optimize-backup"

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "  PVP-OPTIMIZE 构建 + 同步脚本" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "项目: $projectDir"
Write-Host "目标: $dst"
Write-Host ""

# 1. 进入项目目录并构建
Set-Location $projectDir
Write-Host "[1/3] 正在构建..." -ForegroundColor Yellow
$output = & .\gradlew.bat build 2>&1
$buildResult = $LASTEXITCODE

# 输出最后 30 行
$output | Select-Object -Last 30

if ($buildResult -ne 0) {
    Write-Host ""
    Write-Host "[FAILED] 构建失败, jar 未替换" -ForegroundColor Red
    exit 1
}

# 2. 验证 jar 存在
if (-not (Test-Path $src)) {
    Write-Host ""
    Write-Host "[FAILED] 构建成功但找不到 jar: $src" -ForegroundColor Red
    exit 1
}

# 3. 同步到 mods 文件夹
Write-Host ""
Write-Host "[2/3] 正在同步到 PCL mods 文件夹..." -ForegroundColor Yellow

if (-not (Test-Path $modsDir)) {
    Write-Host "[FAILED] mods 文件夹不存在: $modsDir" -ForegroundColor Red
    exit 1
}

# 备份旧版
if (Test-Path $dst) {
    if (-not (Test-Path $backupDir)) {
        New-Item -ItemType Directory -Path $backupDir | Out-Null
    }
    $ts = Get-Date -Format "yyyyMMdd_HHmmss"
    Move-Item $dst (Join-Path $backupDir "$jarName.$ts.bak") -Force
    Write-Host "  旧版已备份到: pvp-optimize-backup/"
}

# 拷贝新版
Copy-Item $src $dst -Force
$info = Get-Item $dst
Write-Host ""
Write-Host "[3/3] 同步完成!" -ForegroundColor Green
Write-Host "  文件: $($info.Name)"
Write-Host "  大小: $($info.Length) bytes"
Write-Host "  时间: $($info.LastWriteTime)"
Write-Host ""
Write-Host "=================================================" -ForegroundColor Green
Write-Host "  可启动游戏测试" -ForegroundColor Green
Write-Host "=================================================" -ForegroundColor Green