package com.aifitnesscoach.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object LayoutSpacing {
    val small = 8.dp
    val screen = 16.dp
    val wideScreen = 24.dp
    val section = 24.dp
    val touchTarget = 48.dp
}

@Composable
fun CenteredContent(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 720.dp)) { content() }
    }
}
