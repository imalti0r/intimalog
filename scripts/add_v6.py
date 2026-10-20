import io, sys
sys.stdout.reconfigure(encoding="utf-8")
p = r"app\src\main\java\com\intima\log\data\db\IntimaDatabase.kt"
s = io.open(p, encoding="utf-8").read()
s = s.replace("version = 5,", "version = 6,")
s = s.replace(
    ".addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)",
    ".addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)",
)
# 追加 MIGRATION_5_6 到 MIGRATION_4_5 定义之后
marker = "private val MIGRATION_4_5"
i = s.find(marker)
if i == -1:
    marker = "val MIGRATION_4_5"
    i = s.find(marker)
# 找该 object 结束（下一个 "^}" 行）
end = s.find("\n}\n", i)
mig = '''

private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // 彻底删除旧枚举保护措施：旧枚举值转中文名并入 customProtections，随后清空两个旧枚举列
        db.execSQL("""
            UPDATE sex_records SET customProtections =
                CASE
                    WHEN customProtections = '' THEN protection
                    ELSE customProtections || ',' || protection
                END
            WHERE protection != 'NOT_RECORDED'
        """)
        db.execSQL("""
            UPDATE sex_records SET customProtections =
                CASE
                    WHEN customProtections = '' THEN protections
                    ELSE customProtections || ',' || protections
                END
            WHERE protections != ''
        """)
        db.execSQL("UPDATE sex_records SET protection = 'NOT_RECORDED', protections = ''")
    }
}
'''
s = s[:end] + mig + s[end:]
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("v6 done")
