import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_q3.xml", encoding="utf-8").read()
print("texts:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x])[:300])
m = re.search(r'text="点击恢复[^"]*"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t)
if m:
    x1, y1, x2, y2 = map(int, m.groups())
    print(f"已删除区: ({(x1+x2)//2},{(y1+y2)//2})")
