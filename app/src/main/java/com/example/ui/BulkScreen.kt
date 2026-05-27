package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BulkScreen(
    viewModel: RemBgViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val queue by viewModel.bulkQueue.collectAsState()
    val isPaused by viewModel.isBulkPaused.collectAsState()
    val isRunning by viewModel.isBulkRunning.collectAsState()

    // Track saved item IDs to synchronize UI states across items and the global "Save All" action
    val savedItemIds = remember { mutableStateListOf<String>() }

    // Multiple Image Picker
    val multipleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // New images added, clear saved image list to reset states
            savedItemIds.clear()
            viewModel.addImagesToBulkQueue(context, uris)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("bulk_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Bulk Processor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BrandSecondary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BrandSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Queue selection/progress control header card
            BulkStatusHeader(
                queue = queue,
                isRunning = isRunning,
                isPaused = isPaused,
                onPauseToggle = { viewModel.setBulkPaused(!isPaused) },
                onStartProcess = { viewModel.startBulkProcessing(context) },
                onClearAll = {
                    savedItemIds.clear()
                    viewModel.clearBulkQueue()
                },
                onPickImages = {
                    multipleImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSaveAllToGallery = {
                    val successQuery = queue.filter { it.status == BulkStatus.SUCCESS && it.processedPath != null }
                    viewModel.saveAllBulkItemsToGallery(context) { count ->
                        if (count > 0) {
                            successQuery.forEach { item ->
                                if (!savedItemIds.contains(item.id)) {
                                    savedItemIds.add(item.id)
                                }
                            }
                            Toast.makeText(context, "Saved $count images to Gallery successfully!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "No completed images to save yet.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            // Horizontal thin divider
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            if (queue.isEmpty()) {
                EmptyBulkQueueLayout(onAddClick = {
                    multipleImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                })
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("bulk_queue_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(queue, key = { _, item -> item.id }) { index, item ->
                        BulkItemRow(
                            index = index,
                            item = item,
                            isSaved = savedItemIds.contains(item.id),
                            onCancel = { viewModel.removeBulkItem(item) },
                            onSave = {
                                viewModel.saveBulkItemToGallery(context, item) {
                                    savedItemIds.add(item.id)
                                    Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BulkStatusHeader(
    queue: List<BulkItem>,
    isRunning: Boolean,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onStartProcess: () -> Unit,
    onClearAll: () -> Unit,
    onPickImages: () -> Unit,
    onSaveAllToGallery: () -> Unit
) {
    val totalCount = queue.size
    val successCount = queue.count { it.status == BulkStatus.SUCCESS }
    val progressPercent = if (totalCount > 0) ((successCount.toFloat() / totalCount) * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(BrandSurfaceLight)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Batch Progress",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$successCount / $totalCount Completed",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = BrandSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(BrandPrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$progressPercent%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Linear continuous progress indicators
        LinearProgressIndicator(
            progress = { if (totalCount > 0) successCount.toFloat() / totalCount else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = BrandPrimary,
            trackColor = Color(0xFFE2E8F0),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (totalCount == 0) {
                // Large picking trigger when empty
                Button(
                    onClick = onPickImages,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("bulk_import_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Images", fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                // Pause resume action caps
                if (isRunning) {
                    IconButton(
                        onClick = onPauseToggle,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = BrandSecondary
                        )
                    }
                }

                // Process trigger button
                Button(
                    onClick = { if (isRunning) onPauseToggle() else onStartProcess() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("bulk_action_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) {
                            if (isPaused) BrandPrimary else Color(0xFFFFB26F)
                        } else BrandPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) {
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause
                        } else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) {
                            if (isPaused) "Resume Batch" else "Pause Batch"
                        } else "Run AI Engine",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Clear/reset queue buttons
                Button(
                    onClick = onClearAll,
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("bulk_clear_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Text("Clear", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (successCount > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSaveAllToGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("bulk_save_all_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Save All",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save All to Gallery ($successCount)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BulkItemRow(
    index: Int,
    item: BulkItem,
    isSaved: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .testTag("bulk_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left profile/URI image thumbnail
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.processedPath ?: item.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandSurfaceLight)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Central details & loading slider
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BrandSecondary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Custom dynamic helper tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (item.status) {
                        BulkStatus.WAITING -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BrandPrimaryLight)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("In Queue", fontSize = 10.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        BulkStatus.PROCESSING -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFFAEB))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("AI Removing...", fontSize = 10.sp, color = Color(0xFFB57E00), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        BulkStatus.SUCCESS -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFECFDF5))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Success", fontSize = 10.sp, color = Color(0xFF047857), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        BulkStatus.FAILED -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Failed", fontSize = 10.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (item.status == BulkStatus.PROCESSING) {
                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = BrandPrimary,
                            trackColor = Color(0xFFF1F5F9)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Actions (check icons or cancel triggers)
            when (item.status) {
                BulkStatus.SUCCESS -> {
                    if (isSaved) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Saved to Gallery",
                            tint = SuccessGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        IconButton(onClick = onSave) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Download Item",
                                tint = BrandPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                BulkStatus.PROCESSING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = BrandPrimary
                    )
                }
                else -> {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyBulkQueueLayout(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BrandSurfaceLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QueuePlayNext,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Add Images to Batch",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BrandSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose up to 12 images from gallery to batch process instantly.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Files", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
