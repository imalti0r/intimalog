import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_name.xml", encoding="utf-8").read()
for m in re.finditer(r'class="android.widget.EditText"[^>]*text="([^"]*)"', t):
    print(f"输入框内容: [{m.group(1)}]")
