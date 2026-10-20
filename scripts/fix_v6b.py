import io, sys
sys.stdout.reconfigure(encoding="utf-8")
p = r"app\src\main\java\com\intima\log\data\db\IntimaDatabase.kt"
s = io.open(p, encoding="utf-8").read()
s = s.replace("WHERE protection != 'NOT_RECORDED'", "WHERE protection NOT IN ('NOT_RECORDED', 'NONE')")
# 追加 NONE → 无措施
s = s.replace('''        db.execSQL("UPDATE sex_records SET protection = 'NOT_RECORDED', protections = ''")''',
'''        db.execSQL("UPDATE sex_records SET customProtections = '无措施' WHERE protection = 'NONE' AND customProtections = ''")
        db.execSQL("UPDATE sex_records SET customProtections = customProtections || ',无措施' WHERE protection = 'NONE' AND customProtections != ''")
        db.execSQL("UPDATE sex_records SET protection = 'NOT_RECORDED', protections = ''")''')
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("migration fixed")
