import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_r4.xml", encoding="utf-8").read()
print("texts:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x])[:120])
m = re.search(r'text="(确定)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t)
if m:
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    print(f"确定: ({(x1+x2)//2},{(y1+y2)//2})")
