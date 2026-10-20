import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_e1.xml", encoding="utf-8").read()
ms = re.findall(r'text="(新增伴侣|\+|＋)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t)
for m in ms:
    x1, y1, x2, y2 = map(int, m[1:])
    print(f"{m[0]}: ({(x1+x2)//2},{(y1+y2)//2})")
