import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_set3.xml", encoding="utf-8").read()
# 找 Switch 节点及其 checked 状态
for m in re.finditer(r'class="[^"]*Switch[^"]*"[^>]*checkable="true"[^>]*checked="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    print(f"Switch checked={m.group(1)}: ({(x1+x2)//2},{(y1+y2)//2})")
