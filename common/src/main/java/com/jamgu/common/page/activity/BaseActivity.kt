package com.jamgu.common.page.activity

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        initWindow()
        super.onCreate(savedInstanceState)

        if (intent.extras == null || checkArgs(intent.extras!!)) {
            onSetContentView(savedInstanceState)
            initWidget()
            initData()
        } else {
            finish()
        }
    }

    /**
     * 可在此初始化控件
     */
    protected open fun initWidget() {}

    /**
     * 可在此初始化数据
     */
    protected open fun initData() {}

    /**
     * 在此设置布局
     */
    protected open fun onSetContentView(savedInstanceState: Bundle?) {}

    /**
     * 在此检查入口参数，若返回false，页面可能直接退出，具体@see [onCreate]
     */
    protected open fun checkArgs(extras: Bundle) = true

    /**
     * 可在此设置window属性
     */
    protected open fun initWindow() {}

    @JvmOverloads
    fun showProgress(msg: String?, loadingDrawableId: Int? = null) {
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

    fun hideProgress() {
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