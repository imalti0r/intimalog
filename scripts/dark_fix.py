import io, re, os

root = r"app\src\main\java\com\intima\log\ui"

# 文字/图标浅色系 → LocalIntimaPalette（深色模式下换亮色）
TEXT_MAP = [
    # (pattern, replacement)
    (re.compile(r"color = Color\(0xFF333333\)"), "color = LocalIntimaPalette.current.text"),
    (re.compile(r"tint = Color\(0xFF333333\)"), "tint = LocalIntimaPalette.current.text"),
    (re.compile(r"else Color\(0xFF333333\)"), "else LocalIntimaPalette.current.text"),
    (re.compile(r"color = Color\(0xCC000000\)"), "color = LocalIntimaPalette.current.text"),
    (re.compile(r"else Color\(0xCC000000\)"), "else LocalIntimaPalette.current.text"),
    (re.compile(r"else Color\(0xFF666666\)"), "else LocalIntimaPalette.current.textSub"),
    (re.compile(r"color = Color\(0xFFAAAAAA\)"), "color = LocalIntimaPalette.current.textHint"),
    (re.compile(r"else Color\(0xFFAAAAAA\)"), "else LocalIntimaPalette.current.textHint"),
    (re.compile(r"tint = Color\(0xFF999999\)"), "tint = LocalIntimaPalette.current.textHint"),
    (re.compile(r"else Color\(0xFF999999\)"), "else LocalIntimaPalette.current.textHint"),
    (re.compile(r"tint = Color\(0xFFCCCCCC\)"), "tint = LocalIntimaPalette.current.textHint"),
    (re.compile(r"else Color\(0xFFCCCCCC\)"), "else LocalIntimaPalette.current.textHint"),
]

# 半透明黑 Chip 底 → palette.chipBg（深色模式下换深灰）
CHIP_MAP = [
    (re.compile(r"Color\(0x10808080\)"), "LocalIntimaPalette.current.chipBg"),
]

# 0x22000000（星星未选 tint）→ 深色下太淡
STAR_MAP = [
    (re.compile(r"Color\(0x22000000\)"), "LocalIntimaPalette.current.textHint.copy(alpha = 0.4f)"),
]

changed = []
for dirpath, _, names in os.walk(root):
    for n in names:
        if not n.endswith(".kt") or n in ("Theme.kt",):
            continue
        p = os.path.join(dirpath, n)
        src = io.open(p, encoding="utf-8").read()
        orig = src
        for pat, rep in TEXT_MAP + CHIP_MAP + STAR_MAP:
            src = pat.sub(rep, src)
        if src != orig:
            if "LocalIntimaPalette" in src and "import com.intima.log.ui.theme.LocalIntimaPalette" not in src:
                lines = src.split("\n")
                last_import = max(i for i, l in enumerate(lines) if l.startswith("import "))
                lines.insert(last_import + 1, "import com.intima.log.ui.theme.LocalIntimaPalette")
                src = "\n".join(lines)
            io.open(p, "w", encoding="utf-8", newline="").write(src)
            changed.append(os.path.relpath(p, root))

print("\n".join(changed))
