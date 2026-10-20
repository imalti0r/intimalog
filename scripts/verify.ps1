#!/usr/bin/env pwsh
# IntimaLog 一键验证脚本：构建 → 安装 → 启动 → 抓 crash → 截图
# 用法: .\scripts\verify.ps1 [-SkipBuild]
param(
    [switch]$SkipBuild
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$artifacts = Join-Path $root "artifacts"
New-Item -ItemType Directory -Force -Path $artifacts | Out-Null
$pkg = "com.intima.log.debug"

Write-Host "== [1/5] 构建 =="
if (-not $SkipBuild) {
    Push-Location $root
    .\gradlew.bat assembleDebug
    Pop-Location
    if ($LASTEXITCODE -ne 0) { Write-Error "assembleDebug 失败"; exit 1 }
}

Write-Host "== [2/5] 安装 =="
adb install -r $apk
if ($LASTEXITCODE -ne 0) { Write-Error "adb install 失败"; exit 1 }

Write-Host "== [3/5] 启动 =="
adb logcat -c
adb shell am start -n "$pkg/com.intima.log.ui.MainActivity"
Start-Sleep -Seconds 4

Write-Host "== [4/5] 崩溃检查 =="
$crash = adb logcat -d -b crash 2>&1 | Out-String
if ($crash -match "FATAL EXCEPTION") {
    Write-Host "检测到崩溃！日志已保存 artifacts\crash.log"
    $crash | Out-File (Join-Path $artifacts "crash.log") -Encoding utf8
    exit 1
} else {
    Write-Host "无崩溃"
}

Write-Host "== [5/5] 截图 =="
adb exec-out screencap -p > (Join-Path $artifacts "home.png")
Write-Host "截图: artifacts\home.png"
Write-Host "验证通过 ✓"
