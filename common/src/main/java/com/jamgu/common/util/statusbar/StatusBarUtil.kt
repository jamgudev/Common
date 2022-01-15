package com.jamgu.common.util.statusbar

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import android.view.WindowManager
import com.jamgu.base.util.JLog
import com.jamgu.common.Common
import com.jamgu.common.R
import com.jamgu.common.util.rom.RomUtils
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Created by jamgu on 2022/01/13
 *
 * 仅支持 API 21 以上
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
object StatusBarUtil {
    private const val TAG = "StatusBarUtil"

    /**
     * 获取机型当前的状态栏高度
     */
    fun getStatusBarHeight(): Int {
        val context: Context = Common.getInstance().getApplicationContext()
        var height = 0
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resId != 0) {
            height = context.resources.getDimensionPixelSize(resId)
        }
        return height
    }

    /**
     * 根据机型，让 自定义的 titleView 适应状态栏的高度
     *
     * @param titleView  如果有必要，该view的高度会被调整。
     *  根据 [isBarInView] 添加 paddingTop，或 topMargin
     * @param isBarInView 是否将status bar 包含在 titleView中
     */
    fun fitStatusLayout(context: Activity?, titleView: View?, isBarInView: Boolean?) {
        context ?: return
        // 设置透明状态栏
        setStatusBarTransparent(context)
        // 资源文件中定义的值
        val definedStatusBarHeight = context.resources
                .getDimensionPixelSize(R.dimen.base_status_bar_height)
        // 通过系统api取到的值
        val apiStatusBarHeight = getStatusBarHeight()
        if (definedStatusBarHeight != 0 && definedStatusBarHeight != apiStatusBarHeight) {
            if (isBarInView == true && null != titleView?.layoutParams) {
                val layoutParams = titleView.layoutParams
                val newPadding = titleView.paddingTop + apiStatusBarHeight
                titleView.setPaddingRelative(
                    titleView.paddingLeft, newPadding,
                    titleView.paddingRight, titleView.paddingBottom
                )
                layoutParams.height += apiStatusBarHeight
                titleView.layoutParams = layoutParams
            } else if (isBarInView != true && null != titleView?.layoutParams) {
                val params = titleView.layoutParams as MarginLayoutParams
                params.topMargin = apiStatusBarHeight
                titleView.layoutParams = params
            }
        }
    }

    /**
     * 是否需要调整状态栏高度，默认是 25 dp，一些手机状态栏高度不是 25 dp
     */
    fun isNeedToUpdateStatusLayout(context: Context): Boolean {
        // 资源文件中定义的值
        val definedStatusBarHeight = context.resources
                .getDimensionPixelSize(R.dimen.base_status_bar_height)
        // 通过系统api取到的值
        val apiStatusBarHeight = getStatusBarHeight()
        return definedStatusBarHeight != 0 && definedStatusBarHeight != apiStatusBarHeight
    }

    /**
     * 设置 activity 状态栏为全透明
     */
    fun setStatusBarTransparent(activity: Activity?) {
        activity ?: return

        val window = activity.window ?: return
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
    }

    /**
     * 修改透明状态栏颜色
     */
    fun setStatusBarColor(activity: Activity?, colorId: Int?) {
        if (activity != null && colorId != null) {
            setStatusBarTransparent(activity)

            val window = activity.window ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.statusBarColor = activity.resources.getColor(colorId, null)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                @Suppress("DEPRECATION")
                window.statusBarColor = activity.resources.getColor(colorId)
            }
        }
    }

    /**
     * 修改状态栏模式
     */
    @Suppress("DEPRECATION")
    fun setStatusBarMode(activity: Activity?, isDark: Boolean?) {
        if (activity == null || isDark == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !RomUtils.isMIUI()) {
            val window = activity.window ?: return
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            if (!isDark) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            } else {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = activity.resources.getColor(R.color.semitransparent)
        } else {
            when {
                RomUtils.isMIUI() -> {
                    MIUIStatusBarHelper.setMIUIStatusBarMode(activity.window, isDark)
                }
                RomUtils.isFlyme() -> {
                    MeiZuStatusBarHelper.setFlymeStatusBarMode(activity.window, isDark)
                }
                else -> {
                    setStatusBarColor(activity, R.color.semitransparent)
                }
            }
        }
    }

    object MeiZuStatusBarHelper {
        private var sMeizuDarkFlagField: Field? = null
        private var sMeizuFlagsField: Field? = null

        @Throws(Throwable::class)
        fun initMeiZuReflectField(window: Window?): Boolean {
            if (sMeizuDarkFlagField == null) {
                sMeizuDarkFlagField = WindowManager.LayoutParams::class.java
                        .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON")
            }
            if (sMeizuFlagsField == null) {
                sMeizuFlagsField = WindowManager.LayoutParams::class.java
                        .getDeclaredField("meizuFlags")
            }

            return sMeizuDarkFlagField != null && sMeizuFlagsField != null
        }

        /**
         * 设置状态栏图标为深色和魅族特定的文字风格
         * 可以用来判断是否为Flyme用户
         *
         * @param window    需要设置的窗口
         * @param dark      是否把状态栏字体及图标颜色设置为深色
         * @return          成功执行返回 true
         */
        fun setFlymeStatusBarMode(window: Window?, dark: Boolean?): Boolean {
            var result = false
            if (window != null) {
                try {
                    val lp = window.attributes
                    if (initMeiZuReflectField(window)) {
                        sMeizuDarkFlagField?.isAccessible = true
                        sMeizuFlagsField?.isAccessible = true
                        val bit = sMeizuDarkFlagField?.getInt(null) ?: return false
                        var value = sMeizuFlagsField?.getInt(lp) ?: return false
                        value = if (dark == true) {
                            value or bit
                        } else {
                            value and bit.inv()
                        }
                        sMeizuFlagsField?.setInt(lp, value)
                        window.attributes = lp
                        result = true
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            JLog.d(TAG, "setFlymeStatusBarMode, result=$result dark=$dark")
            return result
        }
    }

    object MIUIStatusBarHelper {
        private var miuiWMLayoutParamsClazz: Class<*>? = null
        private var miuiDarkModeField: Field? = null
        private var miuiSetExtraFlagMethod: Method? = null

        @SuppressLint("PrivateApi")
        @Throws(Throwable::class)
        fun initMiuiReflectFields(clazz: Class<*>?) {
            clazz ?: return

            if (miuiWMLayoutParamsClazz == null) {
                miuiWMLayoutParamsClazz = Class.forName(
                    "android.view.MiuiWindowManager\$LayoutParams"
                )
            }
            if (miuiDarkModeField == null) {
                miuiDarkModeField = miuiWMLayoutParamsClazz
                        ?.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE")
            }
            if (miuiSetExtraFlagMethod == null) {
                miuiSetExtraFlagMethod = clazz.getMethod(
                    "setExtraFlags",
                    Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                )
            }
        }

        /**
         * 设置状态栏字体图标为深色，需要 MIUI V6 以上
         * @param window    需要设置的窗口
         * @param dark      是否把状态栏字体及图标颜色设置为深色
         * @return          执行成功返回 true
         */
        @Suppress("DEPRECATION")
        fun setMIUIStatusBarMode(window: Window?, dark: Boolean?): Boolean {
            var result = false
            if (window != null) {
                val clazz: Class<*> = window.javaClass
                try {
                    initMiuiReflectFields(clazz)
                    val darkModeFlag: Int = miuiDarkModeField?.getInt(miuiWMLayoutParamsClazz) ?: -1
                    if (dark == true) {
                        // 状态栏透明且黑色字体
                        miuiSetExtraFlagMethod?.invoke(window, darkModeFlag, darkModeFlag)
                    } else {
                        // 清除黑色字体
                        miuiSetExtraFlagMethod?.invoke(window, 0, darkModeFlag)
                    }
                    result = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (dark == true) {
                            window.decorView.systemUiVisibility =
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        } else {
                            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            window.statusBarColor = Common.getInstance().getApplicationContext()
                                    .resources.getColor(R.color.transparent)
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            JLog.d(TAG, "setMIUIStatusBarMode, result=$result dark=$dark")
            return result
        }
    }

}

