package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryLight
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.BrandSurfaceLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = twinSpringspec(1500),
        label = "alpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = twinSpringspec(1200),
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500) // 2.5s display time
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Interactive Concentric AI Logo Node
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .animateContentSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val strokeWidth = 5.dp.toPx()
                    // Inner orbit
                    drawArc(
                        brush = Brush.sweepGradient(listOf(BrandPrimary, BrandPrimaryLight, BrandPrimary)),
                        startAngle = rotationAnim,
                        sweepAngle = 240f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round)
                    )
                    // Outer opposite orbit
                    drawArc(
                        brush = Brush.sweepGradient(listOf(BrandPrimaryLight, BrandPrimary, BrandPrimaryLight)),
                        startAngle = -rotationAnim - 90f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth * 1.2f, cap = StrokeCap.Round)
                    )
                    // Core point
                    drawCircle(
                        color = BrandPrimary,
                        radius = 20.dp.toPx() * alphaAnim,
                        center = center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Premium Typography
            Text(
                text = "BGWRAP",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = BrandSecondary,
                modifier = Modifier.testTag("splash_title")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Professional AI Engine",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = BrandPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun <T> twinSpringspec(duration: Int): SpringSpec<T> {
    return spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingData(
            title = "Precision AI Cutout",
            description = "Erase and replace any image background in a single tap with hair-level edge precision.",
            illustration = OnboardingIllustrationType.REMOVE_BG
        ),
        OnboardingData(
            title = "Bulk Processing Queue",
            description = "Select multiple files at once. Sit back and watch our pipeline batch-render transparency.",
            illustration = OnboardingIllustrationType.BULK
        ),
        OnboardingData(
            title = "Infinite Canvas Replacement",
            description = "Blend cutouts instantly with custom gradients, soft shadows, solid backdrops, or high-quality blur filters.",
            illustration = OnboardingIllustrationType.EXPORT
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("onboarding_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper header controller
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BGWRAP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = BrandSecondary
                )
                TextButton(
                    onClick = { onOnboardingComplete() },
                    modifier = Modifier.testTag("skip_button")
                ) {
                    Text(
                        text = "Skip",
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Interactive horizontal pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val data = pages[page]
                OnboardingPageLayout(data)
            }

            // Footer controller with animated dot slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        val widthAnim by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(widthAnim)
                                .clip(CircleShape)
                                .background(if (isSelected) BrandPrimary else Color.LightGray)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onOnboardingComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("next_onboarding_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (pagerState.currentPage == 2) "Get Started" else "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

enum class OnboardingIllustrationType {
    REMOVE_BG, BULK, EXPORT
}

data class OnboardingData(
    val title: String,
    val description: String,
    val illustration: OnboardingIllustrationType
)

@Composable
fun OnboardingPageLayout(data: OnboardingData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Aesthetic dynamic Graphic visualizer representing the illustrative slide
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(260.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(BrandSurfaceLight),
            contentAlignment = Alignment.Center
        ) {
            when (data.illustration) {
                OnboardingIllustrationType.REMOVE_BG -> IllustrationRemoveBg()
                OnboardingIllustrationType.BULK -> IllustrationBulk()
                OnboardingIllustrationType.EXPORT -> IllustrationExport()
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = data.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BrandSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = data.description,
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 22.sp
        )
    }
}

@Composable
fun IllustrationRemoveBg() {
    val infiniteTransition = rememberInfiniteTransition(label = "crop-onboard")
    val slideAnim by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutBack),
            repeatMode = RepeatMode.Reverse
        ),
        label = "slider"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val rectSize = Size(200.dp.toPx(), 150.dp.toPx())
        val left = (width - rectSize.width) / 2
        val top = (height - rectSize.height) / 2

        // Canvas Photo Box Outline
        drawRoundRect(
            color = Color.LightGray,
            topLeft = Offset(left, top),
            size = rectSize,
            style = Stroke(4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
        )

        // Draw portrait avatar shape inside (subject)
        drawCircle(
            color = Color(0xFFFFB26F),
            radius = 35.dp.toPx(),
            center = Offset(width / 2, height / 2 - 10.dp.toPx())
        )

        // Backdrop side (before background removal representing solid blocks)
        val sliderX = left + (rectSize.width * slideAnim)
        clipRect(left = left, top = top, right = sliderX, bottom = top + rectSize.height) {
            // Flat back block
            drawRect(
                color = BrandPrimaryLight.copy(alpha = 0.6f),
                topLeft = Offset(left, top),
                size = Size(sliderX - left, rectSize.height)
            )
        }

        // Beautiful checkerboard on transparent side (right of sliderX)
        clipRect(left = sliderX, top = top, right = left + rectSize.width, bottom = top + rectSize.height) {
            val checkWidth = 10.dp.toPx()
            for (currX in sliderX.toInt()..(left + rectSize.width).toInt() step checkWidth.toInt()) {
                for (currY in top.toInt()..(top + rectSize.height).toInt() step checkWidth.toInt()) {
                    val isWhite = ((currX / checkWidth.toInt()) + (currY / checkWidth.toInt())) % 2 == 0
                    drawRect(
                        color = if (isWhite) Color.White else Color(0xFFE2E8F0),
                        topLeft = Offset(currX.toFloat(), currY.toFloat()),
                        size = Size(checkWidth, checkWidth)
                    )
                }
            }
        }

        // Draw avatar again clip-safe to stand out
        drawCircle(
            color = Color(0xFF6F74FF),
            radius = 35.dp.toPx(),
            center = Offset(width / 2, height / 2 - 10.dp.toPx()),
            style = Stroke(3f)
        )

        // Before After Slit slider
        drawLine(
            color = BrandPrimary,
            start = Offset(sliderX, top - 10.dp.toPx()),
            end = Offset(sliderX, top + rectSize.height + 10.dp.toPx()),
            strokeWidth = 6f
        )
        drawCircle(
            color = BrandPrimary,
            radius = 10.dp.toPx(),
            center = Offset(sliderX, height / 2)
        )
    }
}

@Composable
fun IllustrationBulk() {
    val transition = rememberInfiniteTransition(label = "pulse-bulk")
    val alpha1 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Picture cards queue simulating stacked pipeline
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = when (index) {
                                    0 -> Color(0xFFEF4444)
                                    1 -> Color(0xFF3B82F6)
                                    else -> Color(0xFF10B981)
                                },
                                radius = 16.dp.toPx(),
                                center = center
                            )
                        }
                        if (index == 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Loading state banner representing queue items
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(BrandPrimaryLight.copy(alpha = alpha1))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI Batch Removing...",
                    color = BrandPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun IllustrationExport() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Central image frame
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(width / 2 - 60.dp.toPx(), height / 2 - 60.dp.toPx()),
            size = Size(120.dp.toPx(), 120.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )

        // Custom shadow layer simulator
        drawOval(
            color = Color.Black.copy(alpha = 0.15f),
            topLeft = Offset(width / 2 - 50.dp.toPx() + 10.dp.toPx(), height / 2 + 50.dp.toPx()),
            size = Size(100.dp.toPx(), 12.dp.toPx())
        )

        // Cutout silhouette inside exports
        drawCircle(
            color = BrandPrimary,
            radius = 24.dp.toPx(),
            center = Offset(width / 2, height / 2 - 10.dp.toPx())
        )

        // Gradient border glow sweep
        drawRoundRect(
            brush = Brush.linearGradient(listOf(BrandPrimary, Color(0xFFEF4444))),
            topLeft = Offset(width / 2 - 60.dp.toPx(), height / 2 - 60.dp.toPx()),
            size = Size(120.dp.toPx(), 120.dp.toPx()),
            style = Stroke(6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )
    }
}
