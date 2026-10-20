import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_s4.xml", encoding="utf-8").read()
for m in re.finditer(r'text="([^"]*)"[^>]*class="android.widget.EditText"[^>]*bounds="(\[[^"]+\])"', t):
    print("EditText:", repr(m.group(1)), m.group(2))
