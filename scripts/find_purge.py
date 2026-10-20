import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_q4.xml", encoding="utf-8").read()
print("dialog:", " | ".join(re.findall(r'text="([^"]{2,40})"', t))[:200])
m = re.search(r'text="(彻底删除)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t)
if m:
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    print(f"彻底删除按钮: ({(x1+x2)//2},{(y1+y2)//2})")
