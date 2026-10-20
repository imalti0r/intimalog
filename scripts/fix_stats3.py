import io, sys
sys.stdout.reconfigure(encoding="utf-8")
p = r"app\src\main\java\com\intima\log\ui\stats\StatsScreen.kt"
s = io.open(p, encoding="utf-8").read()
anchor = "            all.forEach { r ->"
add = '''            fun legacyName(raw: String): String = when (raw.trim()) {
                "NONE" -> "无措施"
                "CONDOM" -> "安全套"
                "PILL_SHORT" -> "短效避孕药"
                "PILL_LONG" -> "长效避孕药"
                "PILL_URGENT", "PILL" -> "紧急避孕药"
                "OTHER" -> "其他"
                else -> raw.trim()
            }

'''
s = s.replace(anchor, add + anchor, 1)
s = s.replace('if (customs.isEmpty()) bump("未记录") else customs.forEach { bump(it) }',
              'if (customs.isEmpty()) bump("未记录") else customs.forEach { bump(legacyName(it)) }')
s = s.replace('if (customs.isEmpty()) bump("未记录") else customs.forEach { bump(it) }',
              'if (customs.isEmpty()) bump("未记录") else customs.forEach { bump(legacyName(it)) }')
# 兜底分支同样
s = s.replace('r.record.protection == "NONE" -> bump("无措施")', 'r.record.protection == "NONE" -> bump("无措施")')
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("legacy mapping ok")
