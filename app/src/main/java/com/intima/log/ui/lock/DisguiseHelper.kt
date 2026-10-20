package com.intima.log.ui.lock

import com.intima.log.data.db.UserProfile

/** 伪装文案辅助 */
object DisguiseHelper {
    fun homeTitle(profile: UserProfile): String =
        if (profile.disguiseEnabled) "日常" else "亲密记录"

    fun multiPartnerLabel(profile: UserProfile): String? =
        if (profile.strongDisguise && profile.disguiseEnabled) "活动" else null
}
