#!/usr/bin/env pwsh
# 抓取崩溃日志到 artifacts\crash.log
$root = Split-Path -Parent $PSScriptRoot
New-Item -ItemType Directory -Force -Path (Join-Path $root "artifacts") | Out-Null
adb logcat -d -b crash | Out-File (Join-Path $root "artifacts\crash.log") -Encoding utf8
Select-String -Path (Join-Path $root "artifacts\crash.log") -Pattern "FATAL EXCEPTION|AndroidRuntime|Caused by" | Select-Object -First 30 | ForEach-Object { $_.Line }
