package com.jamgu.common.widget.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.view.View
import com.jamgu.common.R
import com.jamgu.common.databinding.CommonLoadingDialogLayoutBinding

/**
 * Created by jamgu on 2022/01/21
 *
 */
class CommonProgressDialog private constructor(context: Context, message: String?, loadingDrawable: Drawable?) :
    Dialog(context, R.style.transparent_dialog) {

    private val mBinding: CommonLoadingDialogLayoutBinding = CommonLoadingDialogLayoutBinding.inflate(layoutInflater)

    companion object {
        @JvmStatic
        @JvmOverloads
        fun show(context: Context?, msg: String?, loadingDrawable: Drawable? = null): CommonProgressDialog? {
            context ?: return null

            val dialog = CommonProgressDialog(context, msg, loadingDrawable)
            // 点击返回键是否取消
            dialog.setCancelable(false)
            // 点击外部是否关闭
            dialog.setCanceledOnTouchOutside(false)

            dialog.show()
            return dialog
        }

        @JvmStatic
        fun show(
            context: Context?, msg: String?, loadingDrawable: Drawable?, cancelable: Boolean?,
            onCancelListener: DialogInterface.OnCancelListener?
        ): CommonProgressDialog? {
            context ?: return null

            val dialog = CommonProgressDialog(context, msg, loadingDrawable)
            // 点击返回键是否取消
            dialog.setCancelable(cancelable ?: false)
            // 点击外部是否关闭
            dialog.setCanceledOnTouchOutside(false)
            dialog.setOnCancelListener(onCancelListener)

            dialog.show()
            return dialog
        }

    }

    init {
        setContentView(mBinding.root)
        mBinding.vLoadingTips.text = message
        mBinding.vLoadingTips.visibility = if (message.isNullOrEmpty()) View.GONE else View.VISIBLE
        loadingDrawable?.let {
            mBinding.vLoading.indeterminateDrawable = it
        }
    }

    fun setLoadingMsg(msg: String?) {
        mBinding.vLoadingTips.text = msg
        mBinding.vLoadingTips.visibility = if (msg != null) View.VISIBLE else View.GONE
    }

    fun setLoadingDrawable(drawable: Drawable?) {
        drawable ?: return
        mBinding.vLoading.indeterminateDrawable = drawable
    }

}