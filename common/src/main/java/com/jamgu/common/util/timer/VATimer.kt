package com.jamgu.common.util.timer

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.animation.Animation
import android.view.animation.LinearInterpolator

private const val ONE_SECOND = 1000L
private const val DEFAULT_EXECUTION_TIMES_IN_SINGLE_LOOP = 4

/**
 * Created by jamgu on 2022/02/11
 *
 * 基于ValueAnimator，比[RoughTimer]更精确的计时器
 */
class VATimer {
    private var mTimer: ValueAnimator ? = null

    /**
     * 设置计时器结束时的监听，只会在计时器最后一次重复运行结束时调用
     * 若是一个无限循环的计时器，该监听器无效。
     */
    private var mOnEndListener: ((animation: Animator?) -> Unit)? = null

    /**
     * Timer 开始重复执行时的回调
     */
    private var mOnRepeatListener: ((animation: Animator?) -> Unit)? = null

    /**
     * Timer 当前已循环的次数
     */
    private var mCurrentRepeatCount = 0

    /**
     * Timer 总循环执行次数，设置 0 时不循环，即只执行一次
     * 设置 [Animation.INFINITE] 时 无限循环
     * 默认值为 [Animation.INFINITE]
     */
    private var mTotalRepeatCount = Animation.INFINITE

    /**
     * Timer 单次循环时执行任务的次数，默认为 [DEFAULT_EXECUTION_TIMES_IN_SINGLE_LOOP] 次
     */
    private var mExecutionTimesInSingleLoop = DEFAULT_EXECUTION_TIMES_IN_SINGLE_LOOP

    /**
     * Timer 单次循环的执行总时长，计算公式为
     * mRepeatLength = internal (@see [start]) * [mExecutionTimesInSingleLoop]
     */
    private var mRepeatLength = -1

    /**
     * @see [mTotalRepeatCount]
     */
    fun setTotalRepeatCount(repeatCount: Int) {
        mTotalRepeatCount = repeatCount
    }

    /**
     * @see [mExecutionTimesInSingleLoop]
     */
    fun setExecutionTimesInSingleLoop(executionTimes: Int) {
        mExecutionTimesInSingleLoop = executionTimes
    }

    /**
     * @see [mCurrentRepeatCount]
     */
    fun getCurrentRepeatCount() = mCurrentRepeatCount

    /**
     * @see [mOnEndListener]
     */
    fun addOnEndListener(onEnd: ((animation: Animator?) -> Unit)?) {
        mOnEndListener = onEnd
    }

    /**
     * @see [mOnRepeatListener]
     */
    fun addOnRepeatListener(onEnd: ((animation: Animator?) -> Unit)?) {
        mOnRepeatListener = onEnd
    }

    /**
     * 运行计时器，多次调用start会从头开始
     * @param mission 计时器要运行的任务
     * @param internal 每次mission执行的时间间隔，单位为s
     */
    @SuppressLint("SimpleDateFormat")
    @JvmOverloads
    fun start(mission: () -> Unit, internal: Int = 1) {
        stop()
        // 计算单次循环的执行总时长
        mRepeatLength = internal * mExecutionTimesInSingleLoop

        mTimer = ValueAnimator.ofInt(0, mRepeatLength)

        mTimer?.apply {
            var lastVal = 0
            var passedTime = 0
            var isFirstRunInSingleLoop = true
            var executionTimesInSingleLoop = 0
            addUpdateListener {
                val curVal = it.animatedValue
                if (curVal is Int) {
//                    JLog.d("VATimer", "curVal = $curVal, lastVal = $lastVal")
                    if ((isFirstRunInSingleLoop && passedTime == 0)
                            || (curVal - internal) >= lastVal
                            || (curVal < lastVal) && (passedTime + curVal == internal)) {
                        /*
                        timer 在所有循环即将结束时，会比mExecutionTimesInSingleLoop多执行一次，
                        因此用executionTimesInSingleLoop变量控制
                         */
                        if (executionTimesInSingleLoop < mExecutionTimesInSingleLoop) {
                            mission.invoke()
                            executionTimesInSingleLoop++
                        }
                        lastVal = curVal
                        passedTime = 0
                        isFirstRunInSingleLoop = false
                    } else {
                        /*
                        计算已经过去的时间，在下一个循环开始时用来控制
                         */
                        passedTime = if (curVal > lastVal ) curVal - lastVal else {
                            mRepeatLength + curVal - lastVal
                        }
                    }

                    if (lastVal < 0) lastVal = curVal
                }
            }

            addListener(object : SimpleAnimatorListener() {
                override fun onAnimationEnd(animation: Animator?) {
                    mOnEndListener?.invoke(animation)
                }

                override fun onAnimationRepeat(animation: Animator?) {
                    mCurrentRepeatCount++
                    isFirstRunInSingleLoop = true
                    executionTimesInSingleLoop = 0
                    mOnRepeatListener?.invoke(animation)
                }
            })

            interpolator = LinearInterpolator()
            duration = mRepeatLength * ONE_SECOND
            repeatCount = mTotalRepeatCount
            start()
        }


    }

    /**
     * 停止 Timer
     * 回收资源，释放内存，停止正在运行的Timer
     */
    fun stop() {
        mTimer?.let {
            if (it.isRunning)
                it.pause()
            it.removeAllUpdateListeners()
        }
        mTimer = null
        mCurrentRepeatCount = 0
        mOnEndListener = null
        mOnRepeatListener = null
    }
}

internal open class SimpleAnimatorListener: Animator.AnimatorListener {

    override fun onAnimationStart(animation: Animator?) {
        // do nothing
    }

    override fun onAnimationEnd(animation: Animator?) {
        // do nothing
    }

    override fun onAnimationCancel(animation: Animator?) {
        // do nothing
    }

    override fun onAnimationRepeat(animation: Animator?) {
        // do nothing
    }

}