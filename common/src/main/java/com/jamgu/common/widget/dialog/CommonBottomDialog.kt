package com.jamgu.common.widget.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.view.WindowManager
import android.view.Gravity
import android.widget.TextView
import com.jamgu.common.R
import org.jetbrains.annotations.NotNull

/**
 * 底部弹出的菜单 dialog
 */
class CommonBottomDialog(@NotNull val mActivity: Activity){

    private var vMenusContainer: ViewGroup

    private var mMenuDialog: MenuDialog? = null

    private var mCancelListener: DialogInterface.OnCancelListener? = null

    private val mMenuMap: MutableMap<String, MenuItem> by lazy { LinkedHashMap() }

    private val vPopView: View by lazy {
        LayoutInflater.from(mActivity).inflate(
            R.layout.common_bottom_dialog_layout,
                mActivity.window.decorView as ViewGroup, false)
    }

    fun setOnCancelListener(onCancel: DialogInterface.OnCancelListener?): CommonBottomDialog {
        mCancelListener = onCancel
        return this
    }

    init {
        vMenusContainer = vPopView.findViewById(R.id.image_select_view)
        vPopView.findViewById<View>(R.id.cancel).setOnClickListener {
            mMenuDialog?.cancel()
        }
    }

    @JvmOverloads
    fun addMenuItem(@NotNull id: Int, @NotNull name: String, textColor: String? = "#000000",
                    @NotNull onClick: IMenuItemClick?): CommonBottomDialog {
        return addMenuItem(id.toString(), name, textColor, onClick)
    }

    /**
     * item重复加入的情况：使用最新的
     */
    @JvmOverloads
    fun addMenuItem(@NotNull id: String, @NotNull name: String, textColor: String? = "#000000",
                    @NotNull onClick: IMenuItemClick?): CommonBottomDialog {
        val item = MenuItem(id, name, textColor, onClick)
        mMenuMap[id] = item
        return this
    }

    fun removeMenuItem(@NotNull itemId: String): CommonBottomDialog {
        mMenuMap.remove(itemId)
        return this
    }

    fun clearMenuItems() {
        mMenuMap.clear()
    }

    fun show() {
        if (mMenuMap.isEmpty()) return

        vMenusContainer.removeAllViews()

        for (menuItem in mMenuMap.values) {
            val view: View = createActionItemView(menuItem)
            vMenusContainer.addView(view)
        }

        mMenuDialog = MenuDialog(mActivity, vPopView, mCancelListener)
        mMenuDialog?.show()
    }

    private fun createActionItemView(item: MenuItem): View {
        val view = LayoutInflater.from(mActivity)
                .inflate(R.layout.common_bottom_dialog_item_layout,
                    vMenusContainer, false)

        val vActionName = view.findViewById<TextView>(R.id.action_name)

        vActionName.text = item.menuName
        item.textColor?.let { vActionName.setTextColor(Color.parseColor(it)) }

        vActionName.tag = item.id
        vActionName.setOnClickListener {
            item.onClick?.onClick(it, item)
            mMenuDialog?.dismiss()
        }

        return view
    }

}

/**
 * id, name, textColor(可选), onClick，通过[CommonBottomDialog.addMenuItem]构造
 */
class MenuItem constructor(var id: String, var menuName: String,
                           var textColor: String? = null, var onClick : IMenuItemClick?)

interface IMenuItemClick {
    fun onClick(v: View, item: MenuItem)
}

/**
 * 承接显示菜单的dialog
 */
class MenuDialog(context: Context, var view: View, var onCancel: DialogInterface.OnCancelListener?) :
        Dialog(context,  android.R.style.Theme_Panel) {

    companion object {
        private const val DIM_AMOUNT = 0.8F
    }

    init {
        setContentView(view)
        window?.run {
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            attributes.dimAmount = DIM_AMOUNT

            // 窗口显示屏幕的底部
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)

            // 动画为从底部弹出
            attributes.windowAnimations = android.R.style.Animation_InputMethod
        }
        setCanceledOnTouchOutside(true)

        setOnCancelListener(onCancel)
    }
}
