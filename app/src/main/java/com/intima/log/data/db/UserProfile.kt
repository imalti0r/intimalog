package com.intima.log.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 主人资料（单行表 id=1） */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val gender: Gender = Gender.UNDISCLOSED,
    val birthYear: Int? = null,
    val unitPreference: String = "",       // 单位偏好（预留）
    val statsBasis: StatsBasis = StatsBasis.SESSION,
    val enableMultiPartner: Boolean = true,
    val lockEnabled: Boolean = false,
    val disguiseEnabled: Boolean = false,
    val disguiseIcon: String = "calculator",  // calculator / notes
    val strongDisguise: Boolean = false,   // 强伪装：多人场次显示为中性词
    val screenshotAllowed: Boolean = false,
    val cycleModuleEnabled: Boolean = false,
    val pinHash: String? = null,           // PBKDF2 派生，见 SecurityManager
    val pinSalt: String? = null,
    val weeklyReminder: Boolean = false,
    val onboardingDone: Boolean = false,
)
