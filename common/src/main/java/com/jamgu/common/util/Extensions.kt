package com.jamgu.base.util

import java.lang.StringBuilder
import java.text.DecimalFormat

/**
 * Created by jamgu on 2022/01/12
 */
/**
 * 将小数精确到小数点后几位，向下取整
 */
fun Float.roundToDecimals(decimalPlaces: Int): Float {
    val decimalSb = StringBuilder("0.")
    for (i in 0 until decimalPlaces) {
        decimalSb.append("#")
    }
    val format = DecimalFormat(decimalSb.toString())
    return format.format(this).toFloat()
}