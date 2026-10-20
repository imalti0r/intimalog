import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_n4.xml", encoding="utf-8").read()
for m in re.finditer(r'text="(跟随系统)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    print(f"跟随系统: ({(x1+x2)//2},{(y1+y2)//2})")
