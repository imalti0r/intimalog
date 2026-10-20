import io, re, sys
sys.stdout.reconfigure(encoding="utf-8")
pages = [
    r"app\src\main\java\com\intima\log\ui\partner\PartnerListScreen.kt",
    r"app\src\main\java\com\intima\log\ui\me\TagsManageScreen.kt",
    r"app\src\main\java\com\intima\log\ui\settings\SettingsScreen.kt",
    r"app\src\main\java\com\intima\log\ui\settings\BackupScreen.kt",
    r"app\src\main\java\com\intima\log\ui\partner\CombinationScreens.kt",
    r"app\src\main\java\com\intima\log\ui\template\TemplateScreen.kt",
    r"app\src\main\java\com\intima\log\ui\calendar\CalendarScreen.kt",
]
for p in pages:
    try:
        s = io.open(p, encoding="utf-8").read()
    except FileNotFoundError:
        print("skip", p); continue
    has_bg = ".background(LocalIntimaPalette.current.bg)" in s
    print(("OK  " if has_bg else "MISS"), p)
