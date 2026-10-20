import io, re, sys
sys.stdout.reconfigure(encoding="utf-8")

# BackupManager: draft 里 protection/protections 参数已不存在于 RecordDraft，删除
p = r"app\src\main\java\com\intima\log\data\backup\BackupManager.kt"
s = io.open(p, encoding="utf-8").read()
block_start = s.find("                protection = Protection.NOT_RECORDED,")
block_end = s.find("customProtections = br.customProtections")
if block_start != -1 and block_end != -1:
    s = s[:block_start] + s[block_end:]
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("BackupManager fixed")

# QuickAddActivity: 看损坏区域（160-165 报语法错）
lines = io.open(r"app\src\main\java\com\intima\log\ui\quickadd\QuickAddActivity.kt", encoding="utf-8").read().split("\n")
print("QuickAdd total", len(lines))
for i in range(max(0,len(lines)-25), len(lines)):
    print(i+1, lines[i])
