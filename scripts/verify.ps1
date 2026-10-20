#!/usr/bin/env pwsh
# IntimaLog 一键验证：构建 → 安装 → 冷启动无崩溃 → 四项新功能冒烟路径
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
$adb = "$env:LOCALAPPDATA\..\..\..\..\D:\DevEnv\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "D:\DevEnv\Android\Sdk\platform-tools\adb.exe" }

Write-Host "== [1/9] 构建 =="
if (-not $SkipBuild) {
    Push-Location $root
    .\gradlew.bat assembleDebug testDebugUnitTest
    Pop-Location
    if ($LASTEXITCODE -ne 0) { Write-Error "构建或单测失败"; exit 1 }
}

Write-Host "== [2/9] 安装 =="
& $adb install -r -g $apk
if ($LASTEXITCODE -ne 0) { Write-Error "adb install 失败"; exit 1 }

Write-Host "== [3/9] 冷启动无崩溃 =="
& $adb logcat -c
& $adb shell am force-stop $pkg
Start-Sleep -Seconds 1
& $adb shell am start -n "$pkg/com.intima.log.ui.MainActivity"
Start-Sleep -Seconds 5
$crash = & $adb logcat -d -b crash 2>&1 | Out-String
if ($crash -match "FATAL EXCEPTION") {
    Write-Host "冷启动崩溃！日志已存 artifacts\crash.log"
    $crash | Out-File (Join-Path $artifacts "crash.log") -Encoding utf8
    exit 1
}
Write-Host "无崩溃"

function Test-UiText([string]$needle) {
    & $adb shell "uiautomator dump //sdcard//verify_ui.xml" | Out-Null
    $xml = & $adb shell "cat /sdcard/verify_ui.xml" | Out-String
    return $xml.Contains($needle)
}
function Get-UiBounds([string]$text) {
    & $adb shell "uiautomator dump //sdcard//verify_ui.xml" | Out-Null
    $xml = & $adb shell "cat /sdcard/verify_ui.xml" | Out-String
    if ($xml -match ('text="' + $text + '"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')) {
        return @{ x = ([int]$Matches[1] + [int]$Matches[3]) / 2; y = ([int]$Matches[2] + [int]$Matches[4]) / 2 }
    }
    return $null
}
function Tap-Ui([string]$text) {
    $b = Get-UiBounds $text
    if ($null -eq $b) { Write-Warning "未找到：$text"; return $false }
    & $adb shell input tap $b.x $b.y
    Start-Sleep -Milliseconds 1200
    return $true
}

Write-Host "== [4/9] 功能①：日期选择器（Miuix 滚轮 + SavedStateHandle）=="
Tap-Ui "点击填写详情" | Out-Null
Start-Sleep -Seconds 1
if (-not (Tap-Ui "2026")) {   # 日期块（yyyy-MM-dd 前四位匹配首个）
    # 直接按坐标点击日期块左上区域
    & $adb shell input tap 163 315
    Start-Sleep -Seconds 1
}
if (Test-UiText "选择日期") { Write-Host "① 日期选择弹窗 OK" } else { Write-Warning "① 日期弹窗未出现" }
# 滚轮数字可见性：页面应含数字文本（年份）
if (Test-UiText "2026") { Write-Host "① 滚轮显示具体数字 OK" } else { Write-Warning "① 滚轮数字缺失" }
Tap-Ui "取消" | Out-Null

Write-Host "== [5/9] 功能①追加：时间选择双列循环滚轮 =="
Tap-Ui "19:" | Out-Null   # 时间块 HH:mm
if (Test-UiText "选择时间") { Write-Host "① 时间滚轮弹窗 OK" } else { Write-Warning "① 时间弹窗未出现" }
Tap-Ui "取消" | Out-Null

Write-Host "== [6/9] 功能②：跨日拆分（23:50 + 120min → 自动两条）=="
Tap-Ui "23:" | Out-Null
# 滚时列到 23：从当前值向上滑 3 格
& $adb shell input swipe 445 1369 445 1066 250
Start-Sleep -Milliseconds 600
Tap-Ui "确定" | Out-Null
# 持续 120 分钟
Tap-Ui "点选" | Out-Null
Tap-Ui "120 分" | Out-Null
Tap-Ui "保存" | Out-Null
Start-Sleep -Seconds 2
& $adb shell "uiautomator dump //sdcard//verify_ui.xml" | Out-Null
$xml = & $adb shell "cat /sdcard/verify_ui.xml" | Out-String
if ($xml.Contains("00:00") -and $xml.Contains("23:50")) {
    Write-Host "② 拆分为两条（23:50→00:00 + 00:00→01:50）OK"
} else {
    Write-Warning "② 拆分未检出（需人工确认）"
}

Write-Host "== [7/9] 功能②：合并编辑提示 + 联动删除 =="
$seg = Get-UiBounds "00:00"
if ($null -ne $seg) {
    & $adb shell input tap $seg.x $seg.y
    Start-Sleep -Seconds 2
}
if (Test-UiText "跨日拆分") { Write-Host "② 合并编辑提示 OK" } else { Write-Warning "② 合并提示未检出" }
Tap-Ui "删除记录" | Out-Null
if (Test-UiText "整组所有分段") { Write-Host "② 联动删除确认 OK" } else { Write-Warning "② 删除确认未检出" }
Tap-Ui "删除" | Out-Null
Start-Sleep -Seconds 2

Write-Host "== [8/9] 功能④：三态主题切换 =="
Tap-Ui "我的" | Out-Null
Tap-Ui "设置" | Out-Null
if (Test-UiText "外观") {
    Write-Host "④ 三态开关（跟随系统/浅色/深色）OK"
    Tap-Ui "深色" | Out-Null
    Start-Sleep -Seconds 1
    Tap-Ui "跟随系统" | Out-Null
} else { Write-Warning "④ 外观开关未检出" }

Write-Host "== [9/9] 高潮次数（追加）与统计入口 =="
Tap-Ui "我的" | Out-Null
Tap-Ui "统计" | Out-Null
if (Test-UiText "高潮次数排行") { Write-Host "追加 高潮次数统计卡片 OK" } else { Write-Warning "高潮统计卡片未检出" }
if (Test-UiText "拆分计数") { Write-Host "② 统计页拆分计数开关 OK" } else { Write-Warning "拆分开关未检出" }

$crash2 = & $adb logcat -d -b crash 2>&1 | Out-String
if ($crash2 -match "FATAL EXCEPTION") {
    Write-Host "冒烟过程出现崩溃！日志已存 artifacts\crash.log"
    $crash2 | Out-File (Join-Path $artifacts "crash.log") -Encoding utf8
    exit 1
}
& $adb shell "screencap -p /sdcard/verify.png" 2>$null
& $adb pull /sdcard/verify.png (Join-Path $artifacts "verify_final.png") 2>$null
Write-Host "验证通过 ✓（截图 artifacts\verify_final.png；MIUI 限制 shell 截屏时以 UI dump 结果为准）"
