import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_e1.xml", encoding="utf-8").read()
print("page:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x])[:120])
for m in re.finditer(r'text="([^"]{1,6})"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    txt = m.group(1)
    if txt in ("＋", "+"):
        x1, y1, x2, y2 = map(int, m.groups()[1:])
        print(f"{txt}: ({(x1+x2)//2},{(y1+y2)//2})")
