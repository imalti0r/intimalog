import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_t4.xml", encoding="utf-8").read()
ms = [m for m in re.finditer(r'content-desc="(编辑|铅笔)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t)]
for m in ms[:3]:
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    print(f"{m.group(1)}: ({(x1+x2)//2},{(y1+y2)//2})")
if not ms:
    for m in re.finditer(r'text="12:36"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
        x1, y1, x2, y2 = map(int, m.groups())
        print(f"12:36 行: ({(x1+x2)//2},{(y1+y2)//2})")
