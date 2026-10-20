import io, sys
sys.stdout.reconfigure(encoding="utf-8")
p = r"app\src\main\java\com\intima\log\ui\stats\StatsScreen.kt"
s = io.open(p, encoding="utf-8").read()
old = '''            all.forEach { r ->
                val customs = r.record.customProtections.split(",").filter { it.isNotBlank() }
                if (customs.isEmpty()) bump("未记录") else customs.forEach { bump(it) }
            }'''
new = '''            all.forEach { r ->
                val customs = r.record.customProtections.split(",").filter { it.isNotBlank() }
                when {
                    customs.isNotEmpty() -> customs.forEach { bump(it) }
                    // 防御：v6 迁移前的旧库残留枚举单值
                    r.record.protection == "NONE" -> bump("无措施")
                    else -> bump("未记录")
                }
            }'''
s = s.replace(old, new)
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("stats fallback ok")
