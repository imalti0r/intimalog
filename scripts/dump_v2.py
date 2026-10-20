import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_v2.xml", encoding="utf-8").read()
texts = " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x])
print("stats has 组合排行:", "组合排行" in texts)
