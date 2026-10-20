import io, sys
sys.stdout.reconfigure(encoding="utf-8")
lines = io.open(r"app\src\main\java\com\intima\log\ui\stats\StatsScreen.kt", encoding="utf-8").read().split("\n")
for i in range(133, 152):
    print(i+1, lines[i])
