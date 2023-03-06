package com.jamgu.common.util.timer

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by jamgu on 2022/02/11
 *
 * 基于 ValueAnimator，比[RoughTimer]更精确的计时器
 */
class VATimer @JvmOverloads constructor(timerName: String = "") {
    private var mTimer: ValueAnimator ? = null
    private var mHandlerThread: HandlerThread = HandlerThread("va_timer_thread#$timerName",
        Process.THREAD_PRIORITY_BACKGROUND)

    init {
        mHandlerThread.start()
    }

    companion object {
        private const val DEFAULT_EXECUTION_TIMES_IN_SINGLE_LOOP = 4
    }

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
    private var mSingleRepeatDuration = -1

    @Volatile
    private var mReleased = AtomicBoolean(false)

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
     * 为 Timer 线程设置 UncaughtExceptionHandler
     */
    fun setUncaughtExceptionHandler(eh: Thread.UncaughtExceptionHandler? = null) {
        if (mHandlerThread.isAlive) {
            mHandlerThread.uncaughtExceptionHandler = eh
        }
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

    private fun start(mission: (Int) -> Unit, internal: Int) {
        mSingleRepeatDuration = computeRepeatDuration(internal)

        mTimer = ValueAnimator.ofInt(0, mSingleRepeatDuration)

        mTimer?.apply {
            var lastVal = 0
            var passedTime = 0
            var executionTimes = 0
            var isFirstRunInSingleLoop = true
            var executionTimesInSingleLoop = 0
            addUpdateListener {
                val curVal = it.animatedValue
                if (curVal is Int) {
                    if ((isFirstRunInSingleLoop && passedTime == 0)
                            || (curVal - internal) >= lastVal
                            || (curVal < lastVal) && (passedTime + curVal >= internal)) {
                        /*
                        timer 在所有循环即将结束时，会比mExecutionTimesInSingleLoop多执行一次，
                        因此用executionTimesInSingleLoop变量控制
                         */
                        if (executionTimesInSingleLoop < mExecutionTimesInSingleLoop) {
                            if (!mReleased.get()) {
                                mission.invoke(executionTimes)
                                executionTimes++
                                executionTimesInSingleLoop++
                            }
                        }
                        lastVal = curVal
                        passedTime = 0
                        isFirstRunInSingleLoop = false
                    } else {
                        /*
                        计算已经过去的时间，在下一个循环开始时用来控制
                         */
                        passedTime = if (curVal > lastVal ) curVal - lastVal else {
                            mSingleRepeatDuration + curVal - lastVal
                        }
                    }
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
            duration = mSingleRepeatDuration * 1L
            repeatCount = mTotalRepeatCount
            start()
        }
    }

    /**
     * 运行计时器，每次调用start都会从头开始
     * 任务完成时记得调用 [stop] 停止，否则timer会一直运行下去
     *
     * @param mission 计时器要运行的任务，在子线程运行, 参数Int为mission已经执行的次数
     * @param internal 每次mission执行的时间间隔，单位为ms
     */
    @JvmOverloads
    fun run(mission: (Int) -> Unit, internal: Int = 1) {
        if (isReleased())
            throw RuntimeException("You can not run VATimer in released state.")

        mHandlerThread.let {
            Handler(it.looper).post {
                stop()
                start(mission, internal)
            }
        }
    }

    fun isReleased(): Boolean = mReleased.get()

    fun isRunning(): Boolean = mTimer?.isRunning ?: false

    fun isStarted(): Boolean = mTimer?.isStarted ?: false

    fun isStopped(): Boolean = mTimer?.isPaused ?: true

    /**
     * 停止正在运行的Timer
     */
    fun stop() {
        mTimer?.let {
            if (it.isRunning)
                it.pause()
            it.removeAllUpdateListeners()
            it.removeAllListeners()
        }
        mTimer = null
        mCurrentRepeatCount = 0
        mOnEndListener = null
        mOnRepeatListener = null
    }

    /**
     * 不用时记得调用此方法，终止线程
     * VATimer 调用完该方法后，既完成使命，无法复用旧实例。
     * 若需要使用，需重新创建VATimer对象。
     */
    fun release() {
        stop()
        mHandlerThread.quit()
        mReleased.set(true)
    }

    /**
     * 计算单次循环的执行总时长
     */
    private fun computeRepeatDuration(internal: Int): Int {
        return internal * mExecutionTimesInSingleLoop
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