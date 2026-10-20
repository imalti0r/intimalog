import io, re, sys
sys.stdout.reconfigure(encoding="utf-8")
p = r"app\src\test\java\com\intima\log\StatsLogicTest.kt"
s = io.open(p, encoding="utf-8").read()
s = re.sub(r'^.*import com\.intima\.log\.data\.db\.Protection[^\n]*\n', '', s, flags=re.M)
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("test cleaned")
