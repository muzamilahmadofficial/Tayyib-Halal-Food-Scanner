package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.localization.tStr
import com.example.ui.theme.TayyibDarkGreen
import com.example.ui.theme.TayyibForestGreen
import com.example.ui.theme.TayyibGold
import com.example.ui.theme.TayyibIvory
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.84f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, animationSpec = tween(420))
        delay(900)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(TayyibDarkGreen, TayyibForestGreen, TayyibDarkGreen)
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .scale(scale.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_tayyib_icon_1795716313841),
                contentDescription = tStr(R.string.app_full_name),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(116.dp)
                    .clip(RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "TAYYIB",
                style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 5.sp),
                color = TayyibIvory,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                modifier = Modifier
                    .width(56.dp)
                    .alpha(alpha.value),
                thickness = 1.5.dp,
                color = TayyibGold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tStr(R.string.app_full_name),
                style = MaterialTheme.typography.titleSmall,
                color = TayyibGold,
                modifier = Modifier.alpha(alpha.value),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tStr(R.string.app_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = TayyibIvory.copy(alpha = 0.78f),
                modifier = Modifier.alpha(alpha.value),
                textAlign = TextAlign.Center
            )
        }
    }
}
