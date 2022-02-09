package com.jamgu.common.util.log

import android.util.Log

/**
 * Created by jamgu on 2022/01/12
 */
object JLog {

    /**
     * 日志打印级别，默认打印 v 级别以上的日志
     */
    private var LOG_LEVEL = JLogLevel.LOG_V

    /**
     * 日志开关，默认开启
     */
    private var LOG_ENABLE = true

    @JvmStatic
    fun setEnable(enable: Boolean?) {
        enable ?: return
        LOG_ENABLE = enable
    }

    @JvmStatic
    fun setLogLevel(logLevel: JLogLevel?) {
        logLevel ?: return
        LOG_LEVEL = logLevel
    }

    @JvmStatic
    fun v(tag: String?, msg: String?) {
        msg ?: return

        if (LOG_LEVEL.level >= 5 && LOG_ENABLE) Log.v(tag, msg)
    }

    @JvmStatic
    fun d(tag: String?, msg: String?) {
        msg ?: return

        if (LOG_LEVEL.level >= 4 && LOG_ENABLE) Log.d(tag, msg)
    }

    @JvmStatic
    fun i(tag: String?, msg: String?) {
        msg ?: return

        if (LOG_LEVEL.level >= 3 && LOG_ENABLE) Log.i(tag, msg)
    }

    @JvmStatic
    fun w(tag: String?, msg: String?) {
        w(tag, msg, null)
    }

    @JvmStatic
    fun w(tag: String?, msg: String?, throwable: Throwable?) {
        msg ?: return

        if (LOG_LEVEL.level >= 2 && LOG_ENABLE) {
            if (throwable != null) {
                Log.w(tag, msg, throwable)
            } else {
                Log.w(tag, msg)
            }
        }
    }

    @JvmStatic
    fun e(tag: String?, msg: String?) {
        e(tag, msg, null)
    }

    @JvmStatic
    fun e(tag: String?, msg: String?, throwable: Throwable?) {
        if (msg == null) return

        if (LOG_LEVEL.level >= 1 && LOG_ENABLE) {
            if (throwable != null) {
                Log.e(tag, msg, throwable)
            } else Log.e(tag, msg)
        }
    }
}

/**
 * 日志打印级别，LOG_N，关闭所有日志，
 * LOG_V 打印 V 级别以上的日志，LOG_E 只打印 E 级别的日志，其他以此类推。
 */
enum class JLogLevel(val level: Int) {
    LOG_N(0),
    LOG_V(5),
    LOG_D(4),
    LOG_I(3),
    LOG_W(2),
    LOG_E(1),
}