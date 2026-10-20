import io, re, sys
sys.stdout.reconfigure(encoding="utf-8")
import subprocess
out = subprocess.run(["git","grep","-n","Protection","--","app/src"], capture_output=True, text=True).stdout
print(out)
