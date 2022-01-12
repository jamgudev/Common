package com.jamgu.base.widget

import android.content.Context

/**
 * Created by jamgu on 2022/01/12
 */

/**
 * Int dp 2 px
 */
fun Int.dp2px(context: Context?): Int {
    val scale = getScreenDensity(context)
    return (this * scale + 0.5f).toInt()
}

/**
 * Int px 2 dp
 */
fun Int.px2dp(context: Context?): Int {
    return this.toFloat().px2dp(context)
}

/**
 * Int sp 2 px
 */
fun Int.sp2px(context: Context?): Int {
    return this.toFloat().sp2px(context)
}

/**
 * Int px 2 sp
 */
fun Int.px2sp(context: Context?): Int {
    return this.toFloat().px2sp(context)
}

/**
 * Float dp 2 px
 */
fun Float.dp2px(context: Context?): Int {
    val scale = getScreenDensity(context)
    return (this * scale + 0.5f).toInt()
}

/**
 * Float px 2 dp
 */
fun Float.px2dp(context: Context?): Int {
    val scale = getScreenDensity(context)
    return (this / scale + 0.5f).toInt()
}

/**
 * Float sp 2 px
 */
fun Float.sp2px(context: Context?): Int {
    val scale = getScreenScaledDensity(context)
    return (this * scale + 0.5f).toInt()
}

/**
 * Float px 2 sp
 */
fun Float.px2sp(context: Context?): Int {
    val scale = getScreenScaledDensity(context)
    return (this / scale + 0.5f).toInt()
}

/**
 * 获取屏幕像素缩放因子
 */
fun getScreenDensity(context: Context?): Float {
    context ?: return 0F
    return context.resources.displayMetrics.density
}

/**
 * 获取屏幕字体缩放因子
 */
fun getScreenScaledDensity(context: Context?): Float {
    context ?: return 0F
    return context.resources.displayMetrics.scaledDensity
}
