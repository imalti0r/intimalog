import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_r3.xml", encoding="utf-8").read()
print("texts:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x])[:350])
for label in ("开始时间", "点选"):
    m = re.search(r'text="(%s)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % label, t)
    if m:
        x1, y1, x2, y2 = map(int, m.groups()[1:])
        print(f"{label}: ({(x1+x2)//2},{(y1+y2)//2})")
