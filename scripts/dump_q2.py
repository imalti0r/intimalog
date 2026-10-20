import re, sys
sys.stdout.reconfigure(encoding="utf-8")
t = open(r"artifacts\ui_q1.xml", encoding="utf-8").read()
print("bounds wide:", re.findall(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', t)[-3:])
