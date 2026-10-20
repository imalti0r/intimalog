import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_p4.xml", encoding="utf-8").read()
texts = re.findall(r'text="([^"]*)"', t)
print("texts:", " | ".join([x for x in texts if x])[:550])
plus = [m for m in re.finditer(r'text="＋"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t)]
for i, m in enumerate(plus):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f"＋#{i+1}: ({(x1+x2)//2},{(y1+y2)//2})")
