import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_set2.xml", encoding="utf-8").read()
for m in re.finditer(r'text="允许截屏（默认禁止）"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f"允许截屏行: ({(x1+x2)//2},{(y1+y2)//2})")
