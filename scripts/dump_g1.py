import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_g1.xml", encoding="utf-8").read()
print("page:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x])[:80])
