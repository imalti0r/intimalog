import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_new.xml", encoding="utf-8").read()
# 找 content-desc（图标按钮）
descs = re.findall(r'content-desc="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t)
for d, x1, y1, x2, y2 in descs:
    if d:
        print(f"{d}: ({(int(x1)+int(x2))//2},{(int(y1)+int(y2))//2})")
