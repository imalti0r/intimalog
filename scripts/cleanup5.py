import io, re, sys
sys.stdout.reconfigure(encoding="utf-8")

def rw(p, fn):
    s = io.open(p, encoding="utf-8").read()
    s2 = fn(s)
    io.open(p, "w", encoding="utf-8", newline="").write(s2)
    print("done:", p)

# BackupManager 导入块（格式与我预期不同）——直接看上下文修
def f1(s):
    m = re.search(r'                protection = Protection\.NOT_RECORDED,[^\n]*\n(?:[^\n]*\n)*?customProtections', s)
    if m:
        block = m.group(0)
        newblock = '                protection = "NOT_RECORDED",\n                protections = "",\n                customProtections'
        s = s.replace(block, newblock)
    s = s.replace("item.record.protection.name,", '"NOT_RECORDED",')
    return s
rw(r"app\src\main\java\com\intima\log\data\backup\BackupManager.kt", f1)

def f2(s):
    s = re.sub(r'\n *val protections: List<com\.intima\.log\.data\.db\.Protection> = emptyList\(\),[^\n]*\n', '\n', s)
    return s
rw(r"app\src\main\java\com\intima\log\data\repo\RecordRepository.kt", f2)

def f3(s):
    m = re.search(r'    fun toggleProtection\(p: Protection\) \{\n(?:[^\n]*\n)*?\n', s)
    if m: s = s.replace(m.group(0), "")
    return s
rw(r"app\src\main\java\com\intima\log\ui\record\RecordEditViewModel.kt", f3)
