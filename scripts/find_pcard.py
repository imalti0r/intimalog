import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_p11.xml", encoding="utf-8").read()
print("texts:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x])[:250])
for m in re.finditer(r'text="⋯"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f"menu btn: ({(x1+x2)//2},{(y1+y2)//2})")
