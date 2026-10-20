import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_home2.xml", encoding="utf-8").read()
for m in re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    text, x1, y1, x2, y2 = m.group(1), *map(int, m.groups()[1:])
    cx, cy = (x1+x2)//2, (y1+y2)//2
    if text:
        print(f"{text}: ({cx},{cy})")
