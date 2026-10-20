import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_r3.xml", encoding="utf-8").read()
# 时间胶囊文本是 "10:40"
for m in re.finditer(r'text="(10:40)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    print(f"胶囊10:40: ({(x1+x2)//2},{(y1+y2)//2})")
