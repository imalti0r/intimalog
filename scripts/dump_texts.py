import re, sys, io
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_home2.xml", encoding="utf-8").read()
texts = re.findall(r'text="([^"]+)"', t)
print("\n".join(texts))
