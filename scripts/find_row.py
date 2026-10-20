import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui4.xml", encoding="utf-8").read()
for m in re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    text = m.group(1)
    if "352" in text or "07:58" in text:
        x1, y1, x2, y2 = map(int, m.groups()[1:])
        print(f"{text}: ({(x1+x2)//2},{(y1+y2)//2})")
