package com.jamgu.common.util.timer

import android.os.Handler
import android.os.Looper

/**
 * Created by jamgu on 2021/11/11
 * 基于 Handler 的计时器，会损失一定的精度
 */
class RoughTimer(private val looper: Looper) {

    private var mHandler: Handler? = null

    private lateinit var mMission: Runnable

    private var isRunning: Boolean = false

    fun isStarted(): Boolean {
        return isRunning
    }

    /**
     * @param mission work need to be done.
     * @param delay time that decide how long to delay the execution of tasks.
     */
    fun run(mission: () -> Unit, delay: Long) {
        if (isRunning) return

        mHandler = Handler(looper)

        object : Runnable {
            override fun run() {
                mission.invoke()

                isRunning = true
                mHandler?.postDelayed(this, delay)
            }
        }.also { mMission = it }

        mHandler?.post(mMission)
    }

    /**
     * stop the timer work.
     */
    fun close() {
        isRunning = false
        mHandler?.removeCallbacks(mMission)
        mHandler = null
    }

    /**
     * 不用时记得调用此方法，终止looper
     */
    fun release() {
        close()
        looper.quit()
    }

}