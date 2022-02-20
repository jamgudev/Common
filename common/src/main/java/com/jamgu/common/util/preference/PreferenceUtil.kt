package com.jamgu.common.util.preference

import android.content.Context
import android.content.SharedPreferences
import com.jamgu.common.util.file.security.SecurityUtil

private const val CACHE_NAME = "cache"
private const val GLOBAL = "global"
private const val DEFAULT_NAME = "preference"

object PreferenceUtil {

    fun getPreference(context: Context, uin: Long, name: String?): SharedPreferences {
        var name = name
        if (name == null || name.isEmpty()) {
            name = DEFAULT_NAME
        }
        name = name.replace("/".toRegex(), "%2F")
        val uinStr = if (uin == 0L) GLOBAL else SecurityUtil.encrypt(uin.toString())
        val preferenceName = context.packageName + "_" + uinStr + "_" + name
        return context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    }

    fun getPreference(context: Context, uin: Long, name: String?, version: Float): SharedPreferences {
        var name = name
        if (name == null || name.isEmpty()) {
            name = DEFAULT_NAME
        }
        val uinStr = if (uin == 0L) GLOBAL else SecurityUtil.encrypt(uin.toString())
        val preferenceName = context.packageName + "_" + uinStr + "_" + name + "_" + version
        return context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    }

    fun getCachePreference(context: Context, uin: Long): SharedPreferences {
        return getPreference(context, uin, CACHE_NAME)
    }

    fun getCachePreference(context: Context, uin: Long, version: Float): SharedPreferences {
        return getPreference(context, uin, CACHE_NAME, version)
    }

    fun getDefaultGlobalPreference(context: Context): SharedPreferences {
        return getGlobalPreference(context, null)
    }

    fun getDefaultGlobalPreference(context: Context, version: Float): SharedPreferences {
        return getGlobalPreference(context, null, version)
    }

    fun getGlobalPreference(context: Context, name: String?): SharedPreferences {
        return getPreference(context, 0, name)
    }

    fun getGlobalPreference(context: Context, name: String?, version: Float): SharedPreferences {
        return getPreference(context, 0, name, version)
    }

    fun getGlobalCachePreference(context: Context): SharedPreferences {
        return getGlobalPreference(context, CACHE_NAME)
    }

    fun getGlobalCachePreference(context: Context, version: Float): SharedPreferences {
        return getGlobalPreference(context, CACHE_NAME, version)
    }
}