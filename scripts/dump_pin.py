import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_pin.xml", encoding="utf-8").read()
print("texts:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x]))
for m in re.finditer(r'text="([^"]{1,6})"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    text = m.group(1)
    if text in ("跳过", "完成", "下一步"):
        x1, y1, x2, y2 = map(int, m.groups()[1:])
        print(f"{text}: ({(x1+x2)//2},{(y1+y2)//2})")
