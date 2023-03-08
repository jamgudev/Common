package com.jamgu.common

import android.os.Bundle
import com.jamgu.common.databinding.ActivityMainBinding
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.statusbar.StatusBarUtil
import com.jamgu.common.util.timer.VATimer

class MainActivity : ViewBindingActivity<ActivityMainBinding>() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtil.setStatusBarTransparent(this)
        StatusBarUtil.fitStatusLayout(this, mBinding.toolbar, true)
    }

    val timer = VATimer("aaa")
    override fun onResume() {
        super.onResume()

        timer.addOnEndListener {
            JLog.d("MainActivity", "onEnd.")
        }
        timer.addOnRepeatListener {
            JLog.d("MainActivity", "onRepeat.")
        }
//        timer.setUncaughtExceptionHandler { t, e ->
//            JLog.e(TAG, "threadName = ${t.name}, err = ${e.stackTraceToString()}")
//        }
        timer.run({
//            val i = 1 / 0
            JLog.d("MainActivity", "timer clock. execution = $it repeat = ${timer.getCurrentRepeatCount()}")
        }, 1000)
//        ThreadPool.runUITask({
//            timer.run({
//                JLog.d("MainActivity", "timer clock. execution = $it repeat = ${timer.getCurrentRepeatCount()}")
//            }, 1000)
//        }, 5000)
    }

    override fun getViewBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun onDestroy() {
        super.onDestroy()
        timer.release()
    }
}