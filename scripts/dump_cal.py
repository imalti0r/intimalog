import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_cal.xml", encoding="utf-8").read()
print("texts:", " | ".join([x for x in re.findall(r'text="([^"]*)"', t) if x]))
descs = [d for d in re.findall(r'content-desc="([^"]+)"', t) if d]
print("descs:", " | ".join(descs))
