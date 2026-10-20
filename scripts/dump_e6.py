import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_e5.xml", encoding="utf-8").read()
for m in re.finditer(r'<node[^>]*clickable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    x1, y1, x2, y2 = map(int, m.groups())
    if (x2-x1) < 200 and y1 < 400:
        print(f"clickable: ({(x1+x2)//2},{(y1+y2)//2}) size {(x2-x1)}x{(y2-y1)}")
