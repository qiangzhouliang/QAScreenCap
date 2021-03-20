package com.nanchen.screenrecordhelper

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Switch

/**
 * @desc
 * @author qiangzhouliang
 * @email 2538096489@qq.com
 * @time 2021/3/20 11:38
 * @class QAScreenCap
 * @package com.qzl.qascreencapl
 */
object ViewUtil {
    enum class Margin{
        TOP,BOTTOM,LEFT,RIGHT
    }

    /**
     * @desc 获取位置信息
     * @author QZL
     * @time 2021/3/20 11:39
     */
    @JvmStatic
    fun getLocation(view: View): IntArray {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return location
    }
    @JvmStatic
    fun getViewMargin(context: Context,view: View,margin: Margin): Int {
        val params: ViewGroup.MarginLayoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        var marginValue:Int = 0
        marginValue = when (margin) {
            Margin.TOP -> {
                params.topMargin
            }
            Margin.BOTTOM -> {
                params.bottomMargin
            }
            Margin.LEFT -> {
                params.leftMargin
            }
            Margin.RIGHT -> {
                params.rightMargin
            }
        }

        return marginValue
//        return px2dip(context, marginValue)
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    @JvmStatic
    fun px2dip(context: Context, pxValue: Int): Int {
        val scale: Float = context.getResources().getDisplayMetrics().density
        return (pxValue / scale + 0.5f).toInt()
    }
}