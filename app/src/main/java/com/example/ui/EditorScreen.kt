package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.RectF
import android.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: RemBgViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val activeProj by viewModel.activeProject.collectAsState()
    val originalBmp by viewModel.originalBitmap.collectAsState()
    val cutoutBmp by viewModel.cutoutBitmap.collectAsState()
    val isLoading by viewModel.editorLoading.collectAsState()
    val errorMsg by viewModel.editorError.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()

    // Back door picker if user wants to swap background
    val customBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectCustomBgUri(context, uri)
        }
    }

    var showExportSheet by remember { mutableStateOf(false) }

    // Haptics controller
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    fun performHapticClick() {
        try {
            vibrator?.vibrate(30)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("editor_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = activeProj?.name ?: "Image Editor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    if (originalBmp != null) {
                        IconButton(
                            onClick = {
                                performHapticClick()
                                showExportSheet = true
                            },
                            modifier = Modifier.testTag("export_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = "Share",
                                tint = Color.Black
                            )
                        }
                        
                        Button(
                            onClick = {
                                performHapticClick()
                                showExportSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main Interactive Visual Preview Center (weights 1) - Sleek Floating Style
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFFF8FAFC)) // Soft Elegant light slate (#F8FAFC)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (originalBmp != null) {
                        InteractivePreviewArea(
                            original = originalBmp!!,
                            cutout = cutoutBmp,
                            viewModel = viewModel
                        )
                    } else {
                        CircularProgressIndicator(color = BrandPrimary)
                    }

                    // Sleek design badge
                    if (cutoutBmp != null && !isLoading) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "AI PROCESSING COMPLETE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Progress Loader Banner Overlay
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = BrandPrimary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "AI Server Calculating...",
                                        fontWeight = FontWeight.Bold,
                                        color = BrandSecondary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tracing edges & isolating subjects",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Error Notification HUD
                    if (errorMsg != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(ErrorRed)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = errorMsg!!,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Dismiss",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .clickable { viewModel.editorError.value = null }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }

                // Drag Bottom control sheets (tabs selectors + parameters sliders)
                EditorBottomControlCard(
                    viewModel = viewModel,
                    activeTool = activeTool,
                    cutoutAvailable = cutoutBmp != null,
                    onToolSelected = { tool ->
                        performHapticClick()
                        viewModel.activeTool.value = tool
                    },
                    onTriggerRemoveBg = { viewModel.processBackgroundRemoval() },
                    onBgPickerTrigger = {
                        customBgPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            // Export Settings Bottom Sheet Overlay Drawer
            if (showExportSheet && originalBmp != null) {
                ExportBottomSheet(
                    viewModel = viewModel,
                    subject = cutoutBmp ?: originalBmp!!,
                    onDismiss = { showExportSheet = false },
                    performHaptic = { performHapticClick() }
                )
            }
        }
    }
}

@Composable
fun InteractivePreviewArea(
    original: Bitmap,
    cutout: Bitmap?,
    viewModel: RemBgViewModel
) {
    // Zoom/Pan States
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Before After Split positions
    var splitPosition by remember { mutableFloatStateOf(0.5f) }
    val isComparing = cutout != null && viewModel.activeTool.collectAsState().value == "remove_bg"

    // Background type states
    val bgType by viewModel.backgroundType.collectAsState()
    val solidCol by viewModel.selectedSolidColor.collectAsState()
    val gradCols by viewModel.selectedGradientColors.collectAsState()
    val customBgBmp by viewModel.customBgBitmap.collectAsState()
    val blurVal by viewModel.blurBackgroundAmount.collectAsState()

    // Shadow parameters
    val shadowEnabled by viewModel.shadowEnabled.collectAsState()
    val shOffsetX by viewModel.shadowOffsetX.collectAsState()
    val shOffsetY by viewModel.shadowOffsetY.collectAsState()
    val shRadius by viewModel.shadowRadius.collectAsState()
    val shColor by viewModel.shadowColor.collectAsState()

    // Crop parameters
    val activeTool by viewModel.activeTool.collectAsState()
    val cropLeftVal by viewModel.cropLeft.collectAsState()
    val cropTopVal by viewModel.cropTop.collectAsState()
    val cropRightVal by viewModel.cropRight.collectAsState()
    val cropBottomVal by viewModel.cropBottom.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5.0f)
                    offset += pan
                }
            }
            .clip(RoundedCornerShape(0.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            val width = size.width
            val height = size.height

            // Calculate scaling fit aspect ratio matching image properties
            val imgAspectRatio = original.width.toFloat() / original.height
            val canvasAspectRatio = width / height

            val drawWidth: Float
            val drawHeight: Float

            if (imgAspectRatio > canvasAspectRatio) {
                drawWidth = width * 0.85f
                drawHeight = drawWidth / imgAspectRatio
            } else {
                drawHeight = height * 0.65f
                drawWidth = drawHeight * imgAspectRatio
            }

            val left = (width - drawWidth) / 2 + offset.x
            val top = (height - drawHeight) / 2 + offset.y

            val targetSize = androidx.compose.ui.geometry.Size(drawWidth * scale, drawHeight * scale)
            val targetLeft = left - (targetSize.width - drawWidth) / 2
            val targetTop = top - (targetSize.height - drawHeight) / 2

            // If cutout is not created, or split view slider on left portion: Draw Original
            if (cutout == null) {
                // Display normal original background image
                drawImage(
                    image = original.asImageBitmap(),
                    dstOffset = androidx.compose.ui.unit.IntOffset(targetLeft.toInt(), targetTop.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(targetSize.width.toInt(), targetSize.height.toInt())
                )
            } else {
                // If compare active, we clipping raw vs composed
                val splitBoundaryX = width * splitPosition

                // 1. Draw Transparent grid as backdrop
                val checkWidth = 12.dp.toPx()
                for (currX in targetLeft.toInt()..(targetLeft + targetSize.width).toInt() step checkWidth.toInt()) {
                    for (currY in targetTop.toInt()..(targetTop + targetSize.height).toInt() step checkWidth.toInt()) {
                        val isWhite = ((currX / checkWidth.toInt()) + (currY / checkWidth.toInt())) % 2 == 0
                        drawRect(
                            color = if (isWhite) Color.White else Color(0xFFF1F5F9),
                            topLeft = Offset(currX.toFloat(), currY.toFloat()),
                            size = androidx.compose.ui.geometry.Size(checkWidth, checkWidth)
                        )
                    }
                }

                // 2. Draw Composed replacement background under the cutout limits
                when (bgType) {
                    BgType.SOLID -> {
                        drawRect(
                            color = solidCol,
                            topLeft = Offset(targetLeft, targetTop),
                            size = targetSize
                        )
                    }
                    BgType.GRADIENT -> {
                        drawRect(
                            brush = Brush.linearGradient(gradCols, start = Offset(targetLeft, targetTop), end = Offset(targetLeft, targetTop + targetSize.height)),
                            topLeft = Offset(targetLeft, targetTop),
                            size = targetSize
                        )
                    }
                    BgType.CUSTOM_IMAGE -> {
                        if (customBgBmp != null) {
                            drawImage(
                                image = customBgBmp!!.asImageBitmap(),
                                dstOffset = androidx.compose.ui.unit.IntOffset(targetLeft.toInt(), targetTop.toInt()),
                                dstSize = androidx.compose.ui.unit.IntSize(targetSize.width.toInt(), targetSize.height.toInt())
                            )
                        }
                    }
                    BgType.BLUR -> {
                        // Blurred background simulator
                        drawImage(
                            image = original.asImageBitmap(),
                            dstOffset = androidx.compose.ui.unit.IntOffset(targetLeft.toInt(), targetTop.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(targetSize.width.toInt(), targetSize.height.toInt())
                        )
                        drawRect(
                            color = Color.White.copy(alpha = 0.75f),
                            topLeft = Offset(targetLeft, targetTop),
                            size = targetSize
                        )
                    }
                    BgType.TRANSPARENT -> {
                        // No extra background draw over the checkerboard
                    }
                }

                // 3. Draw Cutout foreground with optional drop shadow
                drawIntoCanvas { canvas ->
                    if (shadowEnabled) {
                        val shPaint = Paint().apply {
                            color = shColor.toArgb()
                            alpha = (0.5f * 255).toInt()
                            if (shRadius > 0f) {
                                maskFilter = BlurMaskFilter(shRadius * scale, BlurMaskFilter.Blur.NORMAL)
                            }
                        }
                        // Draw shifted cutout shadow
                        canvas.nativeCanvas.drawBitmap(
                            cutout,
                            null,
                            RectF(
                                targetLeft + shOffsetX * scale,
                                targetTop + shOffsetY * scale,
                                targetLeft + targetSize.width + shOffsetX * scale,
                                targetTop + targetSize.height + shOffsetY * scale
                            ),
                            shPaint
                        )
                    }

                    // Draw cutout foreground details
                    canvas.nativeCanvas.drawBitmap(
                        cutout,
                        null,
                        RectF(targetLeft, targetTop, targetLeft + targetSize.width, targetTop + targetSize.height),
                        null
                    )
                }

                // 4. Draw Split compare slider mask overlay
                if (isComparing) {
                    clipRect(left = 0f, top = 0f, right = splitBoundaryX, bottom = height) {
                        // Left clipping drawer: overlay original image over everything
                        drawImage(
                            image = original.asImageBitmap(),
                            dstOffset = androidx.compose.ui.unit.IntOffset(targetLeft.toInt(), targetTop.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(targetSize.width.toInt(), targetSize.height.toInt())
                        )
                    }
                }
            }

            // 5. Draw Crop Overlay if crop tool is active
            if (activeTool == "crop") {
                val cropRectLeft = targetLeft + cropLeftVal * targetSize.width
                val cropRectTop = targetTop + cropTopVal * targetSize.height
                val cropRectRight = targetLeft + cropRightVal * targetSize.width
                val cropRectBottom = targetTop + cropBottomVal * targetSize.height

                // Draw dimmed regions outside the crop area
                // Top dimmed band
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(targetLeft, targetTop),
                    size = androidx.compose.ui.geometry.Size(targetSize.width, (cropRectTop - targetTop).coerceAtLeast(0f))
                )
                // Bottom dimmed band
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(targetLeft, cropRectBottom),
                    size = androidx.compose.ui.geometry.Size(targetSize.width, ((targetTop + targetSize.height) - cropRectBottom).coerceAtLeast(0f))
                )
                // Left dimmed band (between top and bottom)
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(targetLeft, cropRectTop),
                    size = androidx.compose.ui.geometry.Size((cropRectLeft - targetLeft).coerceAtLeast(0f), (cropRectBottom - cropRectTop).coerceAtLeast(0f))
                )
                // Right dimmed band (between top and bottom)
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(cropRectRight, cropRectTop),
                    size = androidx.compose.ui.geometry.Size(((targetLeft + targetSize.width) - cropRectRight).coerceAtLeast(0f), (cropRectBottom - cropRectTop).coerceAtLeast(0f))
                )

                // Draw clean white crop bounding stroke
                drawLine(
                    color = Color.White,
                    start = Offset(cropRectLeft, cropRectTop),
                    end = Offset(cropRectRight, cropRectTop),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cropRectLeft, cropRectBottom),
                    end = Offset(cropRectRight, cropRectBottom),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cropRectLeft, cropRectTop),
                    end = Offset(cropRectLeft, cropRectBottom),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cropRectRight, cropRectTop),
                    end = Offset(cropRectRight, cropRectBottom),
                    strokeWidth = 2.dp.toPx()
                )

                // Draw rule-of-thirds lines (low opacity)
                val thirdX1 = cropRectLeft + (cropRectRight - cropRectLeft) / 3f
                val thirdX2 = cropRectLeft + (cropRectRight - cropRectLeft) * 2f / 3f
                val thirdY1 = cropRectTop + (cropRectBottom - cropRectTop) / 3f
                val thirdY2 = cropRectTop + (cropRectBottom - cropRectTop) * 2f / 3f

                // Verticals
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(thirdX1, cropRectTop),
                    end = Offset(thirdX1, cropRectBottom),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(thirdX2, cropRectTop),
                    end = Offset(thirdX2, cropRectBottom),
                    strokeWidth = 1.dp.toPx()
                )
                // Horizontals
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(cropRectLeft, thirdY1),
                    end = Offset(cropRectRight, thirdY1),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(cropRectLeft, thirdY2),
                    end = Offset(cropRectRight, thirdY2),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw premium thick crop handles at corners
                val hLen = 20.dp.toPx()
                val hThick = 4.dp.toPx()

                // Top Left
                drawLine(Color.White, Offset(cropRectLeft - hThick/2, cropRectTop), Offset(cropRectLeft + hLen, cropRectTop), hThick)
                drawLine(Color.White, Offset(cropRectLeft, cropRectTop - hThick/2), Offset(cropRectLeft, cropRectTop + hLen), hThick)

                // Top Right
                drawLine(Color.White, Offset(cropRectRight - hLen, cropRectTop), Offset(cropRectRight + hThick/2, cropRectTop), hThick)
                drawLine(Color.White, Offset(cropRectRight, cropRectTop - hThick/2), Offset(cropRectRight, cropRectTop + hLen), hThick)

                // Bottom Left
                drawLine(Color.White, Offset(cropRectLeft - hThick/2, cropRectBottom), Offset(cropRectLeft + hLen, cropRectBottom), hThick)
                drawLine(Color.White, Offset(cropRectLeft, cropRectBottom - hLen), Offset(cropRectLeft, cropRectBottom + hThick/2), hThick)

                // Bottom Right
                drawLine(Color.White, Offset(cropRectRight - hLen, cropRectBottom), Offset(cropRectRight + hThick/2, cropRectBottom), hThick)
                drawLine(Color.White, Offset(cropRectRight, cropRectBottom - hLen), Offset(cropRectRight, cropRectBottom + hThick/2), hThick)
            }
        }

        // Before After Sliding Split-bar (display only when Compare is active)
        if (isComparing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Drag Slider gesture line target
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(BrandPrimary)
                        .align(Alignment.CenterStart)
                        .offset(x = (((LocalContext.current.resources.displayMetrics.widthPixels / LocalContext.current.resources.displayMetrics.density) * splitPosition) - 2f).dp)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, _, _ ->
                                splitPosition = (splitPosition + pan.x / size.width).coerceIn(0.05f, 0.95f)
                            }
                        }
                ) {
                    // Central floating handle button
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Double tab to reset Zoom indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable {
                    scale = 1f
                    offset = Offset.Zero
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ZoomOutMap, contentDescription = "Reset Zoom", tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("${(scale * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EditorBottomControlCard(
    viewModel: RemBgViewModel,
    activeTool: String?,
    cutoutAvailable: Boolean,
    onToolSelected: (String) -> Unit,
    onTriggerRemoveBg: () -> Unit,
    onBgPickerTrigger: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Drag Sheets Parameters Controls Section
            val sectionHeight = if (activeTool == "crop") 240.dp else 130.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(sectionHeight)
            ) {
                val toolsRequiringCutout = listOf("background", "blur", "shadow")
                if (!cutoutAvailable && activeTool in toolsRequiringCutout) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Background not removed yet", fontWeight = FontWeight.Bold, color = BrandSecondary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onTriggerRemoveBg,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("ai_cutout_trigger")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Isolate Cutout with AI", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                } else if (!cutoutAvailable && activeTool == "remove_bg") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Isolate cutout using spark AI model", fontWeight = FontWeight.Bold, color = BrandSecondary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onTriggerRemoveBg,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("ai_cutout_trigger")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Isolate Cutout with AI", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                } else {
                    this@Column.AnimatedVisibility(
                        visible = activeTool == "remove_bg",
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("BGWRAP AI Cutout Applied ✨", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Use the Before/After split slider to compare details.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    this@Column.AnimatedVisibility(
                        visible = activeTool == "crop",
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        LocalCropRatioSelector(viewModel = viewModel)
                    }

                    this@Column.AnimatedVisibility(
                        visible = activeTool == "background",
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        LocalBackgroundTypeSelector(viewModel = viewModel, onCustomBgClick = onBgPickerTrigger)
                    }

                    this@Column.AnimatedVisibility(
                        visible = activeTool == "blur",
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        LocalBlurSliderSelector(viewModel = viewModel)
                    }

                    this@Column.AnimatedVisibility(
                        visible = activeTool == "shadow",
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        LocalShadowSlidersSelector(viewModel = viewModel)
                    }
                }
            }

            // Quick Tool Swapping Bottom Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ToolTabButton(title = "Remove BG", icon = Icons.Default.AutoAwesome, selected = activeTool == "remove_bg", onClick = { onToolSelected("remove_bg") })
                ToolTabButton(title = "Crop Tool", icon = Icons.Default.Crop, selected = activeTool == "crop", onClick = { onToolSelected("crop") })
                ToolTabButton(title = "Background", icon = Icons.Default.Palette, selected = activeTool == "background", onClick = { onToolSelected("background") })
                ToolTabButton(title = "Blur Filter", icon = Icons.Default.BlurOn, selected = activeTool == "blur", onClick = { onToolSelected("blur") })
                ToolTabButton(title = "Drop Shadow", icon = Icons.Default.Layers, selected = activeTool == "shadow", onClick = { onToolSelected("shadow") })
            }
        }
    }
}

@Composable
fun ToolTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) BrandPrimaryLight else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) BrandPrimary else BrandSecondaryLight,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) BrandPrimary else BrandSecondaryLight
        )
    }
}

@Composable
fun LocalCropRatioSelector(viewModel: RemBgViewModel) {
    val context = LocalContext.current
    val cropLVal by viewModel.cropLeft.collectAsState()
    val cropTVal by viewModel.cropTop.collectAsState()
    val cropRVal by viewModel.cropRight.collectAsState()
    val cropBVal by viewModel.cropBottom.collectAsState()
    val selectedMode by viewModel.selectedCropMode.collectAsState()

    val presets = listOf("Free Size", "Square", "Portrait 4:5", "Story 9:16", "Wide 16:9")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Crop Format Ratio",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BrandSecondary,
            letterSpacing = 0.5.sp
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets) { mode ->
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .background(if (isSelected) BrandPrimaryLight else Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) BrandPrimary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.selectCropPreset(mode) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = mode,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) BrandPrimary else BrandSecondary
                    )
                }
            }
        }

        // Adjustable Sliders
        if (selectedMode == "Free Size") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Custom Crop Limits",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandSecondary,
                    letterSpacing = 0.5.sp
                )
                
                // Row with Left/Right Sliders
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Left: ${(cropLVal * 100).toInt()}%", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = cropLVal,
                            onValueChange = { viewModel.updateCropBounds(it, cropTVal, cropRVal, cropBVal) },
                            valueRange = 0f..0.9f,
                            colors = SliderDefaults.colors(thumbColor = BrandPrimary, activeTrackColor = BrandPrimary)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Right: ${(cropRVal * 100).toInt()}%", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = cropRVal,
                            onValueChange = { viewModel.updateCropBounds(cropLVal, cropTVal, it, cropBVal) },
                            valueRange = 0.1f..1f,
                            colors = SliderDefaults.colors(thumbColor = BrandPrimary, activeTrackColor = BrandPrimary)
                        )
                    }
                }

                // Row with Top/Bottom Sliders
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Top: ${(cropTVal * 100).toInt()}%", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = cropTVal,
                            onValueChange = { viewModel.updateCropBounds(cropLVal, it, cropRVal, cropBVal) },
                            valueRange = 0f..0.9f,
                            colors = SliderDefaults.colors(thumbColor = BrandPrimary, activeTrackColor = BrandPrimary)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bottom: ${(cropBVal * 100).toInt()}%", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = cropBVal,
                            onValueChange = { viewModel.updateCropBounds(cropLVal, cropTVal, cropRVal, it) },
                            valueRange = 0.1f..1f,
                            colors = SliderDefaults.colors(thumbColor = BrandPrimary, activeTrackColor = BrandPrimary)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandPrimaryLight.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aspect Ratio Locked: Centered framing for $selectedMode.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Solid Apply Option
        Button(
            onClick = { viewModel.applyCurrentCrop(context) },
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Icon(Icons.Default.Crop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Apply Selected Crop", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        }
    }
}

data class CropRatioData(val name: String, val bounds: RectF)

@Composable
fun LocalBackgroundTypeSelector(viewModel: RemBgViewModel, onCustomBgClick: () -> Unit) {
    val bgType by viewModel.backgroundType.collectAsState()
    
    val solids = listOf(Color.White, Color.Black, Color(0xFFEF4444), Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFFFBBF24))
    val gradients = listOf(
        listOf(Color(0xFF00C4FF), Color(0xFF0066FF)),
        listOf(Color(0xFFFF5F6D), Color(0xFFFFC371)),
        listOf(Color(0xFF11998e), Color(0xFF38ef7d)),
        listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Canvas Background", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
            
            // Custom BG selector
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrandPrimaryLight)
                    .clickable { onCustomBgClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pick Photo", fontSize = 10.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Category Selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transparent selection node
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, if (bgType == BgType.TRANSPARENT) BrandPrimary else Color.LightGray, CircleShape)
                    .clickable { viewModel.backgroundType.value = BgType.TRANSPARENT },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.GridOn, contentDescription = "Transparent", tint = Color.LightGray, modifier = Modifier.size(18.dp))
            }

            // Solids palette row
            solids.forEach { col ->
                val isSelected = bgType == BgType.SOLID && viewModel.selectedSolidColor.value == col
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(col)
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) BrandPrimary else Color.White, CircleShape)
                        .clickable {
                            viewModel.selectedSolidColor.value = col
                            viewModel.backgroundType.value = BgType.SOLID
                        }
                )
            }

            // Gradients preset rows
            gradients.forEach { list ->
                val isSelected = bgType == BgType.GRADIENT && viewModel.selectedGradientColors.value == list
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(list))
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                        .clickable {
                            viewModel.selectedGradientColors.value = list
                            viewModel.backgroundType.value = BgType.GRADIENT
                        }
                )
            }
        }
    }
}

@Composable
fun LocalBlurSliderSelector(viewModel: RemBgViewModel) {
    val bgType by viewModel.backgroundType.collectAsState()
    val blurVal by viewModel.blurBackgroundAmount.collectAsState()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Original Background Blur", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
            Text("${blurVal.toInt()}%", fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        
        Slider(
            value = blurVal,
            onValueChange = {
                viewModel.blurBackgroundAmount.value = it
                viewModel.backgroundType.value = BgType.BLUR
            },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(thumbColor = BrandPrimary, activeTrackColor = BrandPrimary)
        )
    }
}

@Composable
fun LocalShadowSlidersSelector(viewModel: RemBgViewModel) {
    val shadowEnabled by viewModel.shadowEnabled.collectAsState()
    val radius by viewModel.shadowRadius.collectAsState()
    val offsetVal by viewModel.shadowOffsetX.collectAsState()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Render Drop Shadow", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
            Switch(
                checked = shadowEnabled,
                onCheckedChange = { viewModel.shadowEnabled.value = it },
                colors = SwitchDefaults.colors(checkedThumbColor = BrandPrimary, checkedTrackColor = BrandPrimaryLight)
            )
        }

        if (shadowEnabled) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Blur: ${radius.toInt()}dp", fontSize = 10.sp, color = Color.Gray)
                    Slider(
                        value = radius,
                        onValueChange = { viewModel.shadowRadius.value = it },
                        valueRange = 1f..50f,
                        colors = SliderDefaults.colors(thumbColor = BrandPrimary)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Offset: ${offsetVal.toInt()}px", fontSize = 10.sp, color = Color.Gray)
                    Slider(
                        value = offsetVal,
                        onValueChange = {
                            viewModel.shadowOffsetX.value = it
                            viewModel.shadowOffsetY.value = it
                        },
                        valueRange = 0f..40f,
                        colors = SliderDefaults.colors(thumbColor = BrandPrimary)
                    )
                }
            }
        }
    }
}

@Composable
fun ExportBottomSheet(
    viewModel: RemBgViewModel,
    subject: Bitmap,
    onDismiss: () -> Unit,
    performHaptic: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bgType by viewModel.backgroundType.collectAsState()
    val solidCol by viewModel.selectedSolidColor.collectAsState()
    val gradCols by viewModel.selectedGradientColors.collectAsState()
    val customBmp by viewModel.customBgBitmap.collectAsState()

    var isPngTransparent by remember { mutableStateOf(bgType == BgType.TRANSPARENT) }
    var compressionValue by remember { mutableFloatStateOf(95f) }
    var isSaving by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Top notch bar
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Export Fine-Tuned Output",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = BrandSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure compression format and save to system gallery",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Selector: Transparent PNG vs JPG Backed Color
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                performHaptic()
                                isPngTransparent = true
                            },
                        border = BorderStroke(2.dp, if (isPngTransparent) BrandPrimary else Color(0xFFE2E8F0)),
                        colors = CardDefaults.cardColors(containerColor = if (isPngTransparent) BrandPrimaryLight else Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = if (isPngTransparent) BrandPrimary else Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("PNG (Transparent)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                            Text("Best for graphic assets", fontSize = 9.sp, color = Color.Gray)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                performHaptic()
                                isPngTransparent = false
                            },
                        border = BorderStroke(2.dp, if (!isPngTransparent) BrandPrimary else Color(0xFFE2E8F0)),
                        colors = CardDefaults.cardColors(containerColor = if (!isPngTransparent) BrandPrimaryLight else Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoSizeSelectActual, contentDescription = null, tint = if (!isPngTransparent) BrandPrimary else Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("JPEG (Canvas backed)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                            Text("Best for photos", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Slider Quality ratio
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Export Quality", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                    Text("${compressionValue.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                }
                Slider(
                    value = compressionValue,
                    onValueChange = { compressionValue = it },
                    valueRange = 50f..100f,
                    colors = SliderDefaults.colors(thumbColor = BrandPrimary)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Final save action keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("Cancel", color = BrandSecondary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            isSaving = true
                            performHaptic()
                            
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    // High performance background thread rendering of final Canvas image compilation
                                    val resultBmp = Bitmap.createBitmap(subject.width, subject.height, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(resultBmp)
                                    
                                    if (!isPngTransparent) {
                                        // Compile backing canvas layers manually to bitmap
                                        when (bgType) {
                                            BgType.SOLID -> canvas.drawColor(solidCol.toArgb())
                                            BgType.GRADIENT -> {
                                                val p = Paint()
                                                p.shader = LinearGradient(0f, 0f, 0f, subject.height.toFloat(), gradCols[0].toArgb(), gradCols[1].toArgb(), Shader.TileMode.CLAMP)
                                                canvas.drawRect(0f, 0f, subject.width.toFloat(), subject.height.toFloat(), p)
                                            }
                                            BgType.CUSTOM_IMAGE -> {
                                                customBmp?.let {
                                                    canvas.drawBitmap(it, null, RectF(0f, 0f, subject.width.toFloat(), subject.height.toFloat()), null)
                                                }
                                            }
                                            else -> canvas.drawColor(android.graphics.Color.WHITE)
                                        }
                                    }
                                    
                                    // Draw foreground cutout
                                    canvas.drawBitmap(subject, 0f, 0f, null)
                                    
                                    val resolver = context.contentResolver
                                    val filename = "BGWrap_${System.currentTimeMillis()}.${if (isPngTransparent) "png" else "jpg"}"
                                    val mimeType = if (isPngTransparent) "image/png" else "image/jpeg"
                                    
                                    val contentValues = ContentValues().apply {
                                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/BGWrap")
                                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                                        }
                                    }
                                    
                                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                    var isSavedSuccessfully = false
                                    if (uri != null) {
                                        resolver.openOutputStream(uri).use { out ->
                                            if (out != null) {
                                                isSavedSuccessfully = resultBmp.compress(
                                                    if (isPngTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                                                    compressionValue.toInt(),
                                                    out
                                                )
                                            }
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            contentValues.clear()
                                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                            resolver.update(uri, contentValues, null, null)
                                        }
                                    }
                                    
                                    withContext(Dispatchers.Main) {
                                        isSaving = false
                                        if (isSavedSuccessfully) {
                                            Toast.makeText(context, "Saved to Gallery Successfully!", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Saving to Gallery Failed!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        isSaving = false
                                        Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("save_output_confirm"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Save to Gallery", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
