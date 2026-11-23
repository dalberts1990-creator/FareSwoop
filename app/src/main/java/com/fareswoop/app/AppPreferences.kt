package com.fareswoop.app

import android.content.Context

object AppPreferences {

    private const val PREFS_NAME = "fareswoop_prefs"
    private const val KEY_IS_PRO = "is_pro_user"
    private const val KEY_TERMS_ACCEPTED = "terms_accepted_v1"

    const val FREE_MAX_ACTIVE_TRACKERS = 2
    const val FREE_CHECK_INTERVAL_HOURS = 12L
    const val PRO_FAST_INTERVAL_HOURS = 1L

    fun isPro(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_PRO, false)
    }

    fun setPro(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_PRO, enabled)
            .apply()
    }

    fun hasAcceptedTerms(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_TERMS_ACCEPTED, false)
    }

    fun setTermsAccepted(context: Context, accepted: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TERMS_ACCEPTED, accepted)
            .apply()
    }
}
