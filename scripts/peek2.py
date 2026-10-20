import io, sys
sys.stdout.reconfigure(encoding="utf-8")
lines = io.open(r"app\src\main\java\com\intima\log\data\db\IntimaDatabase.kt", encoding="utf-8").read().split("\n")
for i in range(94, min(140, len(lines))):
    print(i+1, lines[i])
