import io

p = r"app\src\main\java\com\intima\log\data\db\IntimaDatabase.kt"
src = io.open(p, encoding="utf-8").read()
mig = '''/** v1→v2：partners 增加 hidden 列 */
val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE partners ADD COLUMN hidden INTEGER NOT NULL DEFAULT 0")
    }
}

'''
# 从 @Database 之后删除 MIGRATION 块
src = src.replace(mig, "")
# 追加到文件末尾
src = src.rstrip() + "\n\n" + mig.rstrip() + "\n"
io.open(p, "w", encoding="utf-8", newline="").write(src)
print("done")
