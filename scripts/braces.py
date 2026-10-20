import io
s = io.open(r"app\src\main\java\com\intima\log\data\db\IntimaDatabase.kt", encoding="utf-8").read()
depth = 0
for i, ch in enumerate(s):
    if ch == "{": depth += 1
    elif ch == "}":
        depth -= 1
        if depth == 0:
            line = s[:i].count("\n") + 1
            print("class closes at line", line)
            break
