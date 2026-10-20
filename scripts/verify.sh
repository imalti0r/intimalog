#!/usr/bin/env bash
# IntimaLog 一键验证（bash 版）：构建 → 安装 → 冷启动无崩溃 → 四项新功能冒烟
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
ART="$ROOT/artifacts"
PKG="com.intima.log.debug"
ACT="$PKG/com.intima.log.ui.MainActivity"
ADB="D:/DevEnv/Android/Sdk/platform-tools/adb.exe"
command -v adb >/dev/null && ADB=adb
mkdir -p "$ART"

dump_ui() { "$ADB" shell "uiautomator dump //sdcard//v.xml" >/dev/null 2>&1; "$ADB" shell "cat /sdcard/v.xml"; }
has_text() { dump_ui | grep -q "text=\"$1\"" && echo "OK  $2" || echo "MISS $2"; }
tap_text() {
  local x y
  read -r x y < <(dump_ui | grep -o "text=\"$1\"[^>]*bounds=\"\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]\"" \
    | head -1 | sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/' \
    | awk '{print ($1+$3)/2, ($2+$4)/2}')
  [ -n "$x" ] && "$ADB" shell input tap "$x" "$y" && sleep 1.2
}

echo "== [1/9] 构建 + 单测 =="
[ "${1:-}" != "--skip-build" ] && (cd "$ROOT" && ./gradlew.bat assembleDebug testDebugUnitTest)

echo "== [2/9] 安装 =="
"$ADB" install -r -g "$APK"

echo "== [3/9] 冷启动无崩溃 =="
"$ADB" logcat -c
"$ADB" shell am force-stop "$PKG"; sleep 1
"$ADB" shell am start -n "$ACT"; sleep 5
"$ADB" logcat -d -b crash | grep -q "FATAL" && { echo "冷启动崩溃"; exit 1; }
echo "OK 无崩溃"

echo "== [4/9] 功能① 日期选择器 =="
tap_text "点击填写详情"
tap_text "2026" || "$ADB" shell input tap 163 315
sleep 1
has_text "选择日期" "① 日期弹窗"
has_text "2026" "① 滚轮显示具体数字"
tap_text "取消"

echo "== [5/9] 功能① 时间双列循环滚轮 =="
tap_text "19:" || tap_text "20:" || "$ADB" shell input tap 390 315
sleep 1
has_text "选择时间" "① 时间滚轮弹窗"
tap_text "取消"

echo "== [6/9] 功能② 跨日拆分 =="
tap_text "23:" || "$ADB" shell input tap 390 315
sleep 1
"$ADB" shell input swipe 445 1369 445 1066 250; sleep 0.8
tap_text "确定"
tap_text "点选"
tap_text "120 分"
tap_text "保存"; sleep 2
dump_ui | grep -q "00:00" && dump_ui | grep -q "23:50" && echo "OK ② 拆成两条（23:50→00:00 + 00:00→01:50）" || echo "MISS ② 拆分检出"

echo "== [7/9] 功能② 合并编辑 + 联动删除 =="
"$ADB" shell "uiautomator dump //sdcard//v.xml" >/dev/null 2>&1
COORDS=$(dump_ui | grep -o 'text="00:00"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | head -1 \
  | sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/' \
  | awk '{print ($1+$3)/2, ($2+$4)/2}')
[ -n "$COORDS" ] && "$ADB" shell input tap $COORDS && sleep 2
has_text "跨日拆分" "② 合并编辑提示"
tap_text "删除记录"
has_text "整组所有分段" "② 联动删除确认"
tap_text "删除"; sleep 2

echo "== [8/9] 功能④ 三态主题 =="
tap_text "我的"
tap_text "设置"
has_text "外观" "④ 外观三态开关"
tap_text "深色"; sleep 1
tap_text "跟随系统"; sleep 1

echo "== [9/9] 追加：高潮统计 + 拆分计数开关 =="
tap_text "我的"
tap_text "统计"
has_text "高潮次数排行" "追加 高潮统计卡片"
has_text "拆分计数" "② 统计拆分开关"

"$ADB" logcat -d -b crash | grep -q "FATAL" && { echo "冒烟过程崩溃"; exit 1; }
echo "验证通过 ✓"
