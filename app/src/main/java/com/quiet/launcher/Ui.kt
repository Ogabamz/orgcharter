package com.quiet.launcher

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.TextView

/** Small helpers so every screen uses the same type and colours. */
object Ui {
    fun dp(context: Context, v: Int): Int = (v * context.resources.displayMetrics.density).toInt()

    fun serif(context: Context): Typeface = context.resources.getFont(R.font.serif_regular)
    fun serifLight(context: Context): Typeface = context.resources.getFont(R.font.serif_light)
    fun serifItalic(context: Context): Typeface = context.resources.getFont(R.font.serif_italic)

    fun text(
        context: Context,
        value: CharSequence,
        sizeSp: Float,
        colorRes: Int = R.color.fg,
        face: Typeface = serif(context),
    ): TextView = TextView(context).apply {
        text = value
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(context.getColor(colorRes))
        typeface = face
    }

    fun clickable(view: View) {
        val tv = TypedValue()
        view.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
        view.setBackgroundResource(tv.resourceId)
        view.isClickable = true
    }
}
