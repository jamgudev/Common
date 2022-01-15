package com.jamgu.common

import android.annotation.SuppressLint
import android.content.Context

/**
 * Created by jamgu on 2022/01/13
 */
@SuppressLint("StaticFieldLeak")
class Common private constructor(){

    // 防止反射破坏单例
    init {
        if (!flag) {
            flag = true
        } else {
            throw Throwable("SingleTon is being attacked.")
        }
    }

    companion object {
        private var flag = false

        @JvmStatic
        fun getInstance() = CommonSingletonHolder.holder
    }

    private lateinit var sContext: Context

    fun init(context: Context?) {
        if (context == null) {
            throw Throwable("Common Library init failed: context given is null.")
        }
        sContext = context.applicationContext

    }

    fun getApplicationContext(): Context = sContext

    /**
     * 静态内部类单例
     */
    private object CommonSingletonHolder {
        val holder = Common()
    }

}