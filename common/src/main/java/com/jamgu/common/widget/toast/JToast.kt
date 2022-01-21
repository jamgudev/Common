package com.jamgu.common.widget.toast

import android.content.Context
import android.widget.Toast
import com.jamgu.common.thread.ThreadPool

/**
 * Created by jamgu on 2022/01/21
 */
object JToast {

    @JvmOverloads
    fun showToast(context: Context?, msg: String?, showLength: Int = Toast.LENGTH_SHORT) {
        if (context == null || msg.isNullOrEmpty()) return

        if (ThreadPool.isMainThread()) {
            Toast.makeText(context, msg, showLength).show()
        } else {
            ThreadPool.runUITask {
                Toast.makeText(context, msg, showLength).show()
            }
        }
    }

}