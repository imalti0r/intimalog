import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_nav.xml", encoding="utf-8").read()
descs = [(d, (int(x1)+int(x2))//2, (int(y1)+int(y2))//2) for d, x1, y1, x2, y2 in re.findall(r'content-desc="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t)]
print("descs:", " | ".join([f"{d}({x},{y})" for d, x, y in descs if d]))
