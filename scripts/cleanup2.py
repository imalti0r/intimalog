import io, re, sys
sys.stdout.reconfigure(encoding="utf-8")

def rw(p, fn):
    s = io.open(p, encoding="utf-8").read()
    s2 = fn(s)
    io.open(p, "w", encoding="utf-8", newline="").write(s2)
    print("done:", p)

# RecordRepository: draft 字段
def f1(s):
    s = s.replace("val protection: com.intima.log.data.db.Protection = com.intima.log.data.db.Protection.NOT_RECORDED,\n", "")
    s = s.replace("val protections: List<com.intima.log.data.db.Protection> = emptyList(), // 单选保护措施\n", "")
    s = s.replace("suspend fun protectionCounts(): List<com.intima.log.data.db.ProtectionCountRow>\n", "")
    return s
rw(r"app\src\main\java\com\intima\log\data\repo\RecordRepository.kt", f1)

# RecordRepositoryImpl: save 映射去掉 protection/protections；protectionCounts 委托删
def f2(s):
    s = s.replace("protection = draft.protections.firstOrNull() ?: draft.protection,\n", "")
    s = s.replace('protections = draft.protections.joinToString(",") { it.name },\n', "")
    s = s.replace("override suspend fun protectionCounts() = recordDao.protectionCounts()\n", "")
    return s
rw(r"app\src\main\java\com\intima\log\data\repo\RecordRepositoryImpl.kt", f2)

# RecordUseCases: 模板 draft protection 字段
def f3(s):
    s = s.replace("val protection: com.intima.log.data.db.Protection,\n", "")
    s = s.replace("protection = t.protection,\n", "")
    return s
rw(r"app\src\main\java\com\intima\log\domain\usecase\RecordUseCases.kt", f3)

# HomeComponents: 记录行保护措施显示——改为显示 customProtections
def f4(s):
    old = s[s.find("when (item.record.protection)"):]
    end = old.find("}", old.find("}")+1)
    # 找完整 when 块
    m = re.search(r'when \(item\.record\.protection\) \{[^}]*\}', s, re.S)
    if m:
        s = s.replace(m.group(0), 'item.record.customProtections.split(",").filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { "" }')
    return s
rw(r"app\src\main\java\com\intima\log\ui\home\HomeComponents.kt", f4)
