import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_addp3.xml", encoding="utf-8").read()
print("texts:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x])[:300])
for m in re.finditer(r'class="android.widget.EditText"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f"输入框: ({(x1+x2)//2},{(y1+y2)//2})")
