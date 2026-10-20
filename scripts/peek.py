import io, sys
sys.stdout.reconfigure(encoding="utf-8")
for p, a, b in [
    (r"app\src\main\java\com\intima\log\data\backup\BackupManager.kt", 130, 160),
    (r"app\src\main\java\com\intima\log\data\db\IntimaDatabase.kt", 50, 62),
    (r"app\src\main\java\com\intima\log\ui\quickadd\QuickAddActivity.kt", 140, 165),
    (r"app\src\main\java\com\intima\log\ui\record\RecordEditViewModel.kt", 138, 168),
]:
    print("="*20, p, a, b)
    lines = io.open(p, encoding="utf-8").read().split("\n")
    for i in range(a-1, min(b, len(lines))):
        print(i+1, lines[i])
