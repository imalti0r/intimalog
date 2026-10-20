import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_w2.xml", encoding="utf-8").read()
print("texts:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x])[:200])
for m in re.finditer(r'content-desc="(新增)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    print(f"新增: ({(x1+x2)//2},{(y1+y2)//2})")
