package com.jamgu.common.page.activity

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jamgu.common.util.log.JLog
import com.jamgu.common.widget.dialog.CommonProgressDialog
import com.jamgu.common.widget.toast.JToast


/**
 * Created by jamgu on 2022/01/21
 *
 * 最基础的 Activity，封装了 loading progress, toast, 两次返回点击监听
 */
open class BaseActivity: AppCompatActivity() {

    private var mLastBackPressTime = -1L
    private var mDialog: CommonProgressDialog? = null

    companion object {
        private const val TAG = "BaseActivity"
        private const val EXIT_TIME_STAMP = 2000L
    }

    @JvmOverloads
    protected fun showProgress(msg: String?, loadingDrawableId: Int? = null) {
        if (mDialog == null) {
            mDialog = if (loadingDrawableId == null) {
                CommonProgressDialog.show(this, msg)
            } else {
                CommonProgressDialog.show(this, msg, resources.getDrawable(loadingDrawableId, null))
            }
        } else if (mDialog?.isShowing == false) {
            mDialog?.apply {
                setLoadingMsg(msg)
                loadingDrawableId?.let {
                    setLoadingDrawable(resources.getDrawable(it, null))
                }
                show()
            }
        }
    }

    protected fun hideProgress() {
        mDialog?.dismiss()
    }

    @JvmOverloads
    fun showToast(msg: String?, showLength: Int = Toast.LENGTH_SHORT) {
        JToast.showToast(this, msg, showLength)
    }

    /**
     * 用户退出是否需要两次返回，默认不需要
     */
    open fun isBackPressedNeedConfirm() = false

    override fun onBackPressed() {
        if (isBackPressedNeedConfirm()) {
            JLog.d(TAG, "onBackPressed() called.")
            val now = System.currentTimeMillis()
            if (mLastBackPressTime > 0 && now - mLastBackPressTime < EXIT_TIME_STAMP) {
                super.onBackPressed()
            } else {
                showToast("再次点击退出")
                mLastBackPressTime = System.currentTimeMillis()
            }
        } else {
            super.onBackPressed()
        }

    }
}