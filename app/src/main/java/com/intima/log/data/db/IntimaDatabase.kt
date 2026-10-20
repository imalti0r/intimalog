package com.intima.log.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom

/** 枚举转换器 */
class Converters {
    @TypeConverter fun genderToString(g: Gender): String = g.name
    @TypeConverter fun stringToGender(s: String): Gender = Gender.valueOf(s)
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
    version = 1,
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
         * 非正式版：schema 变更直接破坏性重建，不保留迁移链。
         */
        fun build(context: Context, passphrase: ByteArray): IntimaDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context, IntimaDatabase::class.java, DB_NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .fallbackToDestructiveMigration()
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
