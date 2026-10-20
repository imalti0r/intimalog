import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_p1.xml", encoding="utf-8").read()
ms = [m for m in re.finditer(r'content-desc="(编辑)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t)]
if ms:
    x1, y1, x2, y2 = map(int, ms[0].groups()[1:])
    print(f"编辑: ({(x1+x2)//2},{(y1+y2)//2})")
