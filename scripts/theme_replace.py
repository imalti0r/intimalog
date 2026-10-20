import os, re, io, sys
sys.stdout.reconfigure(encoding="utf-8")
root = r"app\src\main\java\com\intima\log\ui"
files = []
for dirpath, _, names in os.walk(root):
    for n in names:
        if n.endswith(".kt") and n not in ("Theme.kt", "Palette.kt"):
            files.append(os.path.join(dirpath, n))

# 模式 → 替换（保守：只动 background(...) 与文字 color= 两种场景）
re_bg = re.compile(r"\.background\(Color\(0xFFF7F7F7\)\)")
re_card = re.compile(r"\.background\(Color\.White\)")
re_card2 = re.compile(r"\.background\(Color\.White, ")
re_text = re.compile(r"color = Color\(0xFF1A1A1A\)")
re_text2 = re.compile(r"color = Color\(0xFF000000\)")
re_hint = re.compile(r"color = Color\(0xFF999999\)")
re_sub = re.compile(r"color = Color\(0xFF666666\)")

for f in files:
    src = io.open(f, encoding="utf-8").read()
    orig = src
    src = re_bg.sub(".background(LocalIntimaPalette.current.bg)", src)
    src = re_card.sub(".background(LocalIntimaPalette.current.card)", src)
    src = re_card2.sub(".background(LocalIntimaPalette.current.card, ", src)
    src = re_text.sub("color = LocalIntimaPalette.current.text", src)
    src = re_text2.sub("color = LocalIntimaPalette.current.text", src)
    src = re_hint.sub("color = LocalIntimaPalette.current.textHint", src)
    src = re_sub.sub("color = LocalIntimaPalette.current.textSub", src)
    if src != orig:
        # 确保 import
        if "LocalIntimaPalette" in src and "import com.intima.log.ui.theme.LocalIntimaPalette" not in src:
            # 插到最后一个 import 后
            lines = src.split("\n")
            last_import = max(i for i, l in enumerate(lines) if l.startswith("import "))
            lines.insert(last_import + 1, "import com.intima.log.ui.theme.LocalIntimaPalette")
            src = "\n".join(lines)
        io.open(f, "w", encoding="utf-8", newline="").write(src)
        print("updated", os.path.relpath(f, root))
