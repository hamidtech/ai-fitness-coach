package com.aifitnesscoach.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aifitnesscoach.R

@Composable
fun TrainingPhotography(height: Dp, modifier: Modifier = Modifier, @DrawableRes art: Int = R.drawable.training_studio) {
    Image(
        painter = painterResource(art),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(20.dp))
    )
}
