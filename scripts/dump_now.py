import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_now.xml", encoding="utf-8").read()
print("texts:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x])[:150])
for m in re.finditer(r'content-desc="(设置|日历|统计)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    print(f"{m.group(1)}: ({(x1+x2)//2},{(y1+y2)//2})")
