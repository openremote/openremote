package io.openremote.orlib.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import io.openremote.orlib.R

class QrScannerView : View {
    private var mPaint: Paint? = null
    private var mStrokePaint: Paint? = null
    private val pTopLeft = Point()
    private val pBotRight = Point()

    constructor(context: Context?) : super(context) {
        initPaints()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initPaints()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initPaints()
    }

    private fun initPaints() {
        mPaint = Paint()
        mPaint!!.color = Color.parseColor("#A6000000")
        mStrokePaint = Paint()
        mStrokePaint!!.color = Color.YELLOW
        mStrokePaint!!.strokeWidth = 4f
        mStrokePaint!!.style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pTopLeft.x = width / 13 + 10
        pTopLeft.y = height / 4 + 20
        pBotRight.x = width - pTopLeft.x - 10
        pBotRight.y = height - pTopLeft.y - 20
        mPaint!!.color = Color.parseColor("#77000000")

        //Top Rect
        canvas.drawRect(0f, 0f, width.toFloat(), pTopLeft.y.toFloat(), mPaint!!)

        //Left Rect
        canvas.drawRect(
            0f,
            pTopLeft.y.toFloat(),
            pTopLeft.x.toFloat(),
            pBotRight.y.toFloat(),
            mPaint!!
        )

        //Right Rect
        canvas.drawRect(
            pBotRight.x.toFloat(),
            pTopLeft.y.toFloat(),
            width.toFloat(),
            pBotRight.y.toFloat(),
            mPaint!!
        )

        //Bottom rect
        canvas.drawRect(0f, pBotRight.y.toFloat(), width.toFloat(), height.toFloat(), mPaint!!)

        //Draw Outer Line drawable
        val d = ContextCompat.getDrawable(context, R.drawable.scanner_outline)
        d?.setBounds(pTopLeft.x, pTopLeft.y, pBotRight.x, pBotRight.y)
        d?.draw(canvas)
    }
}
