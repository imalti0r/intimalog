from PIL import Image
import sys
sys.stdout.reconfigure(encoding="utf-8")
for n in ["g1","g2","g3","g4"]:
    im = Image.open(rf"artifacts\pop_{n}.png").convert("L")
    w, h = im.size
    left = im.crop((0, 0, 100, h))
    right = im.crop((w-100, 0, w, h))
    import statistics
    lv = list(left.getdata()); rv = list(right.getdata())
    print(n, "left_avg=%.0f" % (sum(lv)/len(lv)), "right_avg=%.0f" % (sum(rv)/len(rv)))
