import io, sys
sys.stdout.reconfigure(encoding="utf-8")
p = r"app\src\main\java\com\intima\log\ui\record\RecordEditViewModel.kt"
s = io.open(p, encoding="utf-8").read()
# 重建被误删的两个函数（插在 togglePosition 之前）
anchor = "    fun togglePosition(name: String) {"
add = '''    fun toggleCustomProtection(name: String) {
        state = state.copy(customProtections = state.customProtections.let { if (name in it) it - name else it + name })
    }

    fun setRating(r: Int) { state = state.copy(rating = r) }

'''
s = s.replace(anchor, add + anchor, 1)
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("vm funcs restored")
