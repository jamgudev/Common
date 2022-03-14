package com.jamgu.common.widget.text

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import com.jamgu.common.R

/**
 * 重写onDraw()：使stroke文字描边支持padding
 *
 * 重写onMeasure():
 * 优化：
 * 1.  当width = 1dp | height = 1dp（布局文件中设置的宽高小于我们实际需要的大小）时，内容显示不全
 * 2.  当width = wrap_content && height = wrap_content时，左右两边的描边效果显示不全
 * 【BUG: width = wrap_content && height = match_parent时，仍然还有左右描边显示不全的问题
 *      可以通过向左右两边填充一定的 padding 来解决】
 *
 * 注：宽或高纠正后，[StrokeTextView.setGravity]会被调用【更新为Gravity.CENTER】，布局文件中设置的Gravity会无效
 */
class StrokeTextView : AppCompatTextView {
    private var mStrokePaint: TextPaint? = null
    private var mStrokeWidth = 0f
    private var mTextRect: Rect? = null
    private var mCallCount = 0

    init {
        if (mStrokePaint == null) {
            mStrokePaint = TextPaint()
        }
        if (mTextRect == null) {
            mTextRect = Rect()
        }

        // 复制原来TextView画笔中的一些参数
        val paint = paint
        mStrokePaint?.apply {
            textSize = paint.textSize
            typeface = paint.typeface
            flags = paint.flags
            alpha = paint.alpha

            // 自定义描边效果
            style = Paint.Style.STROKE
            color = resources.getColor(R.color.black)
            strokeWidth = mStrokeWidth
        }

        mStrokeWidth = resources.getDimensionPixelSize(R.dimen.stroke_text_size).toFloat()
    }

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    // 适配：当设置的宽高不够显示时（width = 1dp），自动扩充为wrap_content的区域大小，
    // 并居中显示，同时保留padding(如果有)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mCallCount++
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var modifiedWidth = widthSize
        var modifiedHeight = heightSize
        val text = text.toString()
        // 每次重新计算text的宽度，避免list复用问题
        val textWidth = paint.measureText(text)
        var textHeight = 0f
        textHeight = if (textWidth == 0f) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        } else {
            // 获取text的高度
            val fontMetrics = paint.fontMetrics
            fontMetrics.descent - fontMetrics.top // descent线上面才有文字
        }
        val widthWeNeed = compoundPaddingRight + compoundPaddingLeft +
                textWidth + mStrokeWidth
        // 因为text绘制的特性，文字上面的描边已经足够位置(ascent - top)画了
        // 而bottom - descent之间的区域可能不够 strokeWidth / 2
        val heightWeNeed = compoundPaddingTop + compoundPaddingBottom +
                textHeight + mStrokeWidth / 2
        // specific size or match_parent，but we only handle specific size here
        if (widthMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.EXACTLY) {

            // 第二次onMeasure()时，传回来的widthMode = MeasureSpec.EXACTLY
            // 会导致最终Stroke的描边位置不准确，所以这里控制第二次onMeasure()时，不对width进行特殊处理
            if (widthMode == MeasureSpec.EXACTLY && mCallCount < 2) {
                modifiedWidth = widthSize.toFloat().coerceAtLeast(widthWeNeed).toInt()

                // 如果值没有改变，说明是match_parent，足够位置显示，所以不特殊处理，否则居中显示
                if (modifiedWidth != widthSize) {
                    gravity = Gravity.CENTER
                }
            }
            if (heightMode == MeasureSpec.EXACTLY) {
                modifiedHeight = heightSize.toFloat().coerceAtLeast(heightWeNeed).toInt()

                // 如果值没有改变，说明是match_parent，足够位置显示，所以不特殊处理，否则居中显示
                if (modifiedHeight != heightSize) {
                    gravity = Gravity.CENTER
                }
            }
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(modifiedWidth, widthMode),
                MeasureSpec.makeMeasureSpec(modifiedHeight, heightMode)
            )
            //            setMeasuredDimension(modifiedWidth, modifiedHeight);
        } else if (widthMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.AT_MOST) {
            if (widthMode == MeasureSpec.AT_MOST) {
                modifiedWidth = widthWeNeed.toInt()
                gravity = Gravity.CENTER
            }
            if (heightMode == MeasureSpec.AT_MOST) {
                modifiedHeight = heightWeNeed.toInt()
                gravity = Gravity.CENTER
            }

//            setMeasuredDimension(modifiedWidth, modifiedHeight);
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(modifiedWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(modifiedHeight, MeasureSpec.EXACTLY)
            )
        }

        // mCallCount = 2时重置，避免list复用问题
        mCallCount %= 2
    }

    public override fun onDraw(canvas: Canvas) {
        val text = text.toString()
        if (!TextUtils.isEmpty(text)) {

            //在文本底层画出带描边的文本
            val gravity = gravity

            val strokePaint = mStrokePaint ?: return
            // 优化描边位置的计算，同时支持左、右padding
            when {
                gravity and Gravity.LEFT == Gravity.LEFT -> {
                    canvas.drawText(
                        text, compoundPaddingLeft.toFloat(),
                        baseline.toFloat(), strokePaint
                    )
                }
                gravity and Gravity.RIGHT == Gravity.RIGHT -> {
                    canvas.drawText(
                        text, width - compoundPaddingRight - paint.measureText(text),
                        baseline.toFloat(), strokePaint
                    )
                }
                else -> {
                    // 除去左、右padding后，在剩下的空间中paint落笔的位置
                    val xInLeftSpace = (width - compoundPaddingRight - compoundPaddingLeft
                            - paint.measureText(text)) / 2
                    // 最终落笔点位置 [x = paddingLeft + xInLeftSpace, y = getBaseLine()]
                    canvas.drawText(
                        text, paddingLeft + xInLeftSpace,
                        baseline.toFloat(), strokePaint
                    )
                }
            }
        }
        super.onDraw(canvas)
    }

    override fun setTypeface(tf: Typeface?) {
        // 模仿TextView的设置
        // 需在super.setTypeface()调用之前，不然没有效果
        if (mStrokePaint != null && mStrokePaint?.typeface !== tf) {
            mStrokePaint?.typeface = tf
        }
        super.setTypeface(tf)
    }

    override fun setTypeface(tf: Typeface?, style: Int) {
        var tfl = tf ?: return
        if (style > 0) {
            tfl = Typeface.create(tfl, style)
            typeface = tfl
            // now compute what (if any) algorithmic styling is needed
            val typefaceStyle = tfl?.style ?: 0
            val need = style and typefaceStyle.inv()
            paint.isFakeBoldText = need and Typeface.BOLD != 0
            paint.textSkewX = if (need and Typeface.ITALIC != 0) -0.25f else 0f

            // 同步设置mStrokeTextPaint
            mStrokePaint?.apply {
                isFakeBoldText = need and Typeface.BOLD != 0
                textSkewX = if (need and Typeface.ITALIC != 0) -0.25f else 0f
            }
        } else {
            paint.isFakeBoldText = false
            paint.textSkewX = 0f

            // 同步设置mStrokeTextPaint
            mStrokePaint?.apply {
                isFakeBoldText = false
                textSkewX = 0f
            }

            typeface = tfl
        }
    }
}