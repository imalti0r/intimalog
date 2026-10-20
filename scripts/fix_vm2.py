import io, sys
sys.stdout.reconfigure(encoding="utf-8")
p = r"app\src\main\java\com\intima\log\ui\record\RecordEditViewModel.kt"
s = io.open(p, encoding="utf-8").read()
anchor = "    fun toggleCustomProtection(name: String) {"
add = '''    fun setMood(m: String) { state = state.copy(moodTag = m) }

    fun toggleCustomPlace(name: String) {
        state = state.copy(customPlaces = state.customPlaces.let { if (name in it) it - name else it + name })
    }

'''
s = s.replace(anchor, add + anchor, 1)
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("ok")
