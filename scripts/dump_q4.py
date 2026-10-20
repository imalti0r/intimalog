import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_q4.xml", encoding="utf-8").read()
print("地点页:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x])[:120])
