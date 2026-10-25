package com.example.drishti.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.drishti.R

/**
 * DrishtiEyeLogo — Renders the authoritative DRISHTI-AI logo.
 * Includes options for full logo (eye + text) or emblem only.
 */
@Composable
fun DrishtiEyeLogo(
    modifier: Modifier = Modifier,
    width: Dp = 180.dp,
    height: Dp = 100.dp,
    showText: Boolean = false
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        val drawableRes = if (showText) R.drawable.drishti_logo_full else R.drawable.drishti_logo_eye
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "DRISHTI-AI Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width, height)
        )
    }
}
