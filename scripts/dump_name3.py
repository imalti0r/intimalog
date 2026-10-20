import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_name3.xml", encoding="utf-8").read()
for m in re.finditer(r'text="(A|[^"]{0,12})"[^>]*class="android.widget.EditText"', t):
    print(f"EditText: [{m.group(1)}]")
