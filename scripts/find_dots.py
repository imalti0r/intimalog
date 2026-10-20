import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_x1.xml", encoding="utf-8").read()
# 找第一个 ⋯
ms = [m for m in re.finditer(r'text="⋯"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t)]
if ms:
    x1, y1, x2, y2 = map(int, ms[0].groups())
    print(f"第一个⋯: ({(x1+x2)//2},{(y1+y2)//2})")
