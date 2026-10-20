import io, re, sys
sys.stdout.reconfigure(encoding="utf-8")

def rw(p, fn):
    s = io.open(p, encoding="utf-8").read()
    s2 = fn(s)
    io.open(p, "w", encoding="utf-8", newline="").write(s2)
    print("done:", p)

# RecordEditViewModel: protections 字段、toggleProtection、旧枚举迁移逻辑删
def f1(s):
    s = s.replace("val protections: Set<Protection> = emptySet(),  // 多选保护措施（预置枚举）\n", "")
    s = s.replace('''    fun toggleProtection(p: Protection) {
        state = state.copy(protections = state.protections.let { if (p in it) it - p else it + p })
    }

''', "")
    # 旧枚举迁移块：existing.record.protections 枚举解析 + protection 单值——删掉，只留 custom
    old = s[s.find("customProtections = ("):]
    end = old.find("),")
    block = '''customProtections = (
                        existing.record.protections.split(",").filter { it.isNotBlank() }
                            .mapNotNull { n -> runCatching { Protection.valueOf(n) }.getOrNull() }
                            .ifEmpty { listOf(existing.record.protection) }
                            .map { it.label() } +
                        existing.record.customProtections.split(",").filter { it.isNotBlank() }
                        ).distinct().toSet(),'''
    s = s.replace(block, "customProtections = existing.record.customProtections.split(\",\").filter { it.isNotBlank() }.toSet(),")
    # save 里 protections = emptyList() 行删
    s = s.replace("protections = emptyList(),\n", "")
    return s
rw(r"app\src\main\java\com\intima\log\ui\record\RecordEditViewModel.kt", f1)

# RecordEditScreen: label() 扩展删（ Protection 枚举已不存在）
def f2(s):
    m = re.search(r'fun Protection\.label\(\): String = when \(this\) \{[^}]*\}\n', s, re.S)
    if m: s = s.replace(m.group(0), "")
    return s
rw(r"app\src\main\java\com\intima\log\ui\record\RecordEditScreen.kt", f2)

# StatsScreen: 删 protectionColor / protectionDisplayName(Protection)，按名取色保留；统计循环去枚举解析
def f3(s):
    m = re.search(r'    private fun protectionColor\(p: Protection\): Color = when \(p\) \{[^}]*\}\n\n', s, re.S)
    if m: s = s.replace(m.group(0), "")
    m = re.search(r'    private fun protectionDisplayName\(p: Protection\): String = when \(p\) \{[^}]*\}\n', s, re.S)
    if m: s = s.replace(m.group(0), "")
    # 统计循环：去掉枚举解析
    old = s[s.find("            all.forEach { r ->"):]
    end = old.find("            }")
    block = '''            all.forEach { r ->
                val enums = r.record.protections.split(",").filter { it.isNotBlank() }
                    .mapNotNull { n -> runCatching { Protection.valueOf(n) }.getOrNull() }
                val customs = r.record.customProtections.split(",").filter { it.isNotBlank() }
                if (enums.isEmpty() && customs.isEmpty()) {
                    bump(protectionDisplayName(r.record.protection))
                } else {
                    enums.forEach { bump(protectionDisplayName(it)) }
                    customs.forEach { bump(it) }
                }
            }'''
    new = '''            all.forEach { r ->
                val customs = r.record.customProtections.split(",").filter { it.isNotBlank() }
                if (customs.isEmpty()) bump("未记录") else customs.forEach { bump(it) }
            }'''
    s = s.replace(block, new)
    return s
rw(r"app\src\main\java\com\intima\log\ui\stats\StatsScreen.kt", f3)

# BackupManager: 导出 protection.name 改常量；导入时旧枚举名转中文并入 custom
def f4(s):
    s = s.replace("protection = r.record.protection.name,", 'protection = "NOT_RECORDED",')
    old = s[s.find("protection = Protection.NOT_RECORDED,"):]
    end = old.find("},")
    block = '''protection = Protection.NOT_RECORDED, // 兼容旧导出；多选 protections 字段为权威
                protections = br.protections.split(",").filter { it.isNotBlank() }
                    .map { n ->
                        when (n) {
                            "PILL" -> Protection.PILL_URGENT
                            else -> runCatching { Protection.valueOf(n) }.getOrNull()
                        }
                    }
                        .filterNotNull()
                        .joinToString(",") { it.label() }
                        .ifBlank { br.protections.split(",").filter { it.isNotBlank() }.joinToString(",") }'''
    new = '''protection = "NOT_RECORDED",
                protections = "",'''
    s = s.replace(block, new)
    return s
rw(r"app\src\main\java\com\intima\log\data\backup\BackupManager.kt", f4)

# QuickAddActivity: 删 protection 状态与 chips 区
def f5(s):
    s = s.replace("private var protection by mutableStateOf(Protection.NOT_RECORDED)\n", "")
    s = s.replace("protection = protection,\n", "")
    s = s.replace("onProtection = { protection = it },\n", "")
    s = s.replace("protection = protection,\n", "")
    m = re.search(r'protection: Protection,\n', s)
    if m: s = s.replace(m.group(0), "")
    m = re.search(r'onProtection: \(Protection\) -> Unit,\n', s)
    if m: s = s.replace(m.group(0), "")
    # chips 区整块（Protection.entries.forEach 到闭合）——找到 SectionTitle("保护措施") 区块
    m = re.search(r'[^\n]*保护措施[^\n]*\n(.*?\n)?\n', s)
    # 简化：删 Protection.entries.forEach 所在整块
    m = re.search(r'\n\s*Protection\.entries\.forEach \{ p ->\n(?:[^\n]*\n)*?\s*\}\n', s)
    if m: s = s.replace(m.group(0), "\n")
    return s
rw(r"app\src\main\java\com\intima\log\ui\quickadd\QuickAddActivity.kt", f5)

# TemplateScreen: protection 参数与显示
def f6(s):
    s = s.replace("fun save(name: String, partnerIds: List<Long>, protection: com.intima.log.data.db.Protection, rating: Int, place: com.intima.log.data.db.Place) {",
                  "fun save(name: String, partnerIds: List<Long>, rating: Int, place: com.intima.log.data.db.Place) {")
    s = s.replace("protection = protection,\n", "")
    s = s.replace('append(" · ${template.protection.label()}")\n', "")
    return s
rw(r"app\src\main\java\com\intima\log\ui\template\TemplateScreen.kt", f6)
