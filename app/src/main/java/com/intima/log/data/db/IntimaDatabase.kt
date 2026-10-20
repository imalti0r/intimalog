package com.intima.log.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom

/** 枚举转换器 */
class Converters {
    @TypeConverter fun genderToString(g: Gender): String = g.name
    @TypeConverter fun stringToGender(s: String): Gender = Gender.valueOf(s)
    @TypeConverter fun protectionToString(p: Protection): String = p.name
    @TypeConverter fun stringToProtection(s: String): Protection = Protection.valueOf(s)
    @TypeConverter fun placeToString(p: Place): String = p.name
    @TypeConverter fun stringToPlace(s: String): Place = Place.valueOf(s)
    @TypeConverter fun basisToString(b: StatsBasis): String = b.name
    @TypeConverter fun stringToBasis(s: String): StatsBasis = StatsBasis.valueOf(s)
}

@Database(
    entities = [
        Partner::class,
        SexRecord::class,
        RecordPartnerCrossRef::class,
        UserProfile::class,
        CycleLog::class,
        SceneTemplate::class,
        CustomTagEntity::class,
    ],
    version = 5,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class IntimaDatabase : RoomDatabase() {
    abstract fun partnerDao(): PartnerDao
    abstract fun recordDao(): RecordDao
    abstract fun profileDao(): ProfileDao
    abstract fun cycleDao(): CycleDao
    abstract fun templateDao(): TemplateDao
    abstract fun tagDao(): TagDao

    companion object {
        const val DB_NAME = "intima.db"

        /**
         * 创建 SQLCipher 加密数据库。
         * 密钥由 Android Keystore 派生（见 SecurityManager），存于 EncryptedSharedPreferences。
         */
        fun build(context: Context, passphrase: ByteArray): IntimaDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context, IntimaDatabase::class.java, DB_NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration(false)
                .build()
        }
    }
}

/** 随机生成 32 字节数据库口令 */
fun generateDbPassphrase(): ByteArray {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return bytes
}

/** v1→v2：partners 增加 hidden 列 */
val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE partners ADD COLUMN hidden INTEGER NOT NULL DEFAULT 0")
    }
}

/** v2→v3：sex_records 增加多选保护措施 / 体位 / 玩具列 */
val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sex_records ADD COLUMN protections TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE sex_records ADD COLUMN positions TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE sex_records ADD COLUMN toys TEXT NOT NULL DEFAULT ''")
    }
}

/** v3→v4：custom_tags 表（体位/玩具集中管理） */
val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS custom_tags (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "kind TEXT NOT NULL, " +
                "name TEXT NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_custom_tags_kind_name ON custom_tags(kind, name)")
        // 旧 PILL 枚举 → 三分类（protection 与 protections 两列都转）
        db.execSQL("UPDATE sex_records SET protection = 'PILL_URGENT' WHERE protection = 'PILL'")
        db.execSQL(
            "UPDATE sex_records SET protections = 'PILL_URGENT' " +
                "WHERE protections = 'PILL' OR protections LIKE 'PILL,%' OR protections LIKE '%,PILL' OR protections LIKE '%,PILL,%'"
        )
    }
}

/** v4→v5：records 增加自定义保护措施 / 自定义地点列 */
val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sex_records ADD COLUMN customProtections TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE sex_records ADD COLUMN customPlaces TEXT NOT NULL DEFAULT ''")
    }
}
