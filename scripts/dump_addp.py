import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_addp.xml", encoding="utf-8").read()
# 找输入框与按钮位置
for m in re.finditer(r'text="([^"]*)"[^>]*class="android.widget.EditText"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    print(f"输入框[{m.group(1)}]: ({(x1+x2)//2},{(y1+y2)//2})")
for m in re.finditer(r'text="(保存)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t):
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    print(f"保存: ({(x1+x2)//2},{(y1+y2)//2})")
