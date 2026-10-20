import io, re, sys
sys.stdout.reconfigure(encoding="utf-8")

def sub(p, old, new, count=1):
    s = io.open(p, encoding="utf-8").read()
    if old not in s:
        print("MISS:", p, repr(old[:60])); return
    s = s.replace(old, new, count)
    io.open(p, "w", encoding="utf-8", newline="").write(s)
    print("ok:", p)

# Partner.kt 删枚举与扩展
p = r"app\src\main\java\com\intima\log\data\db\Partner.kt"
s = io.open(p, encoding="utf-8").read()
s = re.sub(r'enum class Protection[^\n]*\n', '', s)
s = re.sub(r'fun Protection\.labelOrLegacy\(\)[^\n]*\n', '', s)
# displayLabel 整个 when 块删
m = re.search(r'fun Protection\.displayLabel\(\): String = when \(this\) \{[^}]*\}\n', s, re.S)
if m: s = s.replace(m.group(0), "")
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("Partner.kt cleaned")

# SexRecord.kt: protection 字段改 String
p = r"app\src\main\java\com\intima\log\data\db\SexRecord.kt"
s = io.open(p, encoding="utf-8").read()
s = s.replace('val protection: Protection = Protection.NOT_RECORDED,', 'val protection: String = "NOT_RECORDED",   // 仅存兼容旧数据，新记录恒 "NOT_RECORDED"')
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("SexRecord.kt cleaned")

# SceneTemplate.kt: protection 改 String
p = r"app\src\main\java\com\intima\log\data\db\SceneTemplate.kt"
s = io.open(p, encoding="utf-8").read()
s = s.replace('val protection: Protection = Protection.NOT_RECORDED,', 'val protection: String = "NOT_RECORDED",')
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("SceneTemplate.kt cleaned")

# IntimaDatabase.kt: 删 TypeConverter 两个
p = r"app\src\main\java\com\intima\log\data\db\IntimaDatabase.kt"
s = io.open(p, encoding="utf-8").read()
s = re.sub(r'\s*@TypeConverter fun protectionToString[^\n]*\n', '\n', s)
s = re.sub(r'\s*@TypeConverter fun stringToProtection[^\n]*\n', '\n', s)
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("IntimaDatabase.kt cleaned")

# Daos.kt: protectionCounts 查询删（统计已不用）
p = r"app\src\main\java\com\intima\log\data\db\Daos.kt"
s = io.open(p, encoding="utf-8").read()
s = s.replace('''    @Query("SELECT protection, COUNT(*) AS cnt FROM sex_records GROUP BY protection")
    suspend fun protectionCounts(): List<ProtectionCountRow>''', '')
s = re.sub(r'data class ProtectionCountRow[^\n]*\n', '', s)
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("Daos.kt cleaned")
