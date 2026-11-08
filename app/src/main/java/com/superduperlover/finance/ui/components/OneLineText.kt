package com.superduperlover.finance.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Text that always stays on ONE line: no wrapping, and the font shrinks in 0.5sp steps (down to [minFontSize])
 * when the width is too small, e.g. with the phone's 1.25 font scale. Use it for nav, tabs, chips, tiles, buttons, amounts.
 * The largest size is the size of [style].
 */
@Composable
fun OneLineText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    minFontSize: TextUnit = 10.sp,
) {
    val max = if (style.fontSize.isSpecified) style.fontSize else MaterialTheme.typography.bodyMedium.fontSize
    val min = if (minFontSize.value < max.value) minFontSize else max
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = min, maxFontSize = max, stepSize = 0.5.sp),
    )
}
