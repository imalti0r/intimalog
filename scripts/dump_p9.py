import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_p9.xml", encoding="utf-8").read()
print("me:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x])[:350])
m = re.search(r'text="(保护措施管理)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t)
if m:
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    print(f"保护措施管理: ({(x1+x2)//2},{(y1+y2)//2})")
