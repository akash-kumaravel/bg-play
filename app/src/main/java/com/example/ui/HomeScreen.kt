package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ProjectEntity
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: RemBgViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToBulk: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projectsState.collectAsState()
    
    var pendingInitialTool by remember { mutableStateOf("remove_bg") }
    
    // Media Pickers
    val singleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.createProjectFromUri(context, uri, pendingInitialTool) {
                onNavigateToEditor()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pendingInitialTool = "remove_bg"
                    singleImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                containerColor = BrandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .testTag("import_fab")
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Import Image",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Greeting Header (Time of Day Dependent)
            HomeGreetingHeader()

            // Large Upload Action Card
            UploadActionCard(
                onUploadClick = {
                    pendingInitialTool = "remove_bg"
                    singleImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            // Dynamic Recent Projects Horizontal Carousel
            RecentProjectsSection(
                projects = projects,
                onProjectClick = { proj ->
                    viewModel.loadProject(proj)
                    onNavigateToEditor()
                },
                onDeleteProject = { proj ->
                    viewModel.deleteProject(proj)
                }
            )

            // Spark AI / Quick Tools Grid Table
            QuickToolsSection(
                onNavigateToBulk = onNavigateToBulk,
                onQuickImport = {
                    pendingInitialTool = "crop"
                    singleImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
fun HomeGreetingHeader() {
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "$greeting,",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Welcome to BGWRAP ✨",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = BrandSecondary
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BrandPrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = "Premium Mode",
                tint = BrandPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun UploadActionCard(onUploadClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(180.dp)
            .clickable { onUploadClick() }
            .testTag("upload_image_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BrandPrimaryLight.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Brush.sweepGradient(listOf(BrandPrimary, AccentBlue, BrandPrimary)), RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap to Remove Background",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Upload any photo for high precision AI cutout",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun RecentProjectsSection(
    projects: List<ProjectEntity>,
    onProjectClick: (ProjectEntity) -> Unit,
    onDeleteProject: (ProjectEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Projects",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BrandSecondary
            )
            if (projects.isNotEmpty()) {
                Text(
                    text = "${projects.size} items",
                    fontSize = 13.sp,
                    color = BrandPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (projects.isEmpty()) {
            EmptyProjectsLayout()
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recent_projects_list")
            ) {
                items(projects, key = { it.id }) { project ->
                    RecentProjectCard(
                        project = project,
                        onClick = { onProjectClick(project) },
                        onDelete = { onDeleteProject(project) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecentProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("project_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = BrandSurfaceLight)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Show cutout preview if available, otherwise display original thumbnail
            val displayPath = project.processedImagePath ?: project.originalImagePath
            
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(displayPath)
                    .crossfade(true)
                    .build(),
                contentDescription = project.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )

            // Transparent dark bottom shade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (project.processedImagePath != null) "PNG Cutout" else "Original",
                            color = if (project.processedImagePath != null) AccentBlue else Color.LightGray,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyProjectsLayout() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(BrandSurfaceLight)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterFrames,
                contentDescription = "Empty",
                tint = Color.LightGray,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No saved projects yet",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandSecondaryLight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cutouts you create will be automatically saved here.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun QuickToolsSection(
    onNavigateToBulk: () -> Unit,
    onQuickImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "AI Workflows",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BrandSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bulk Remove Card (Quick Tool)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onNavigateToBulk() }
                    .testTag("bulk_tool_card"),
                colors = CardDefaults.cardColors(containerColor = BrandSurfaceLight),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BurstMode,
                            contentDescription = "Bulk",
                            tint = BrandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Bulk Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandSecondary
                        )
                        Text(
                            text = "Batch remove backgrounds",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Quick crop / transparency creator card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onQuickImport() },
                colors = CardDefaults.cardColors(containerColor = BrandSurfaceLight),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = "Crop Studio",
                            tint = BrandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Crop Tool",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandSecondary
                        )
                        Text(
                            text = "Resize and align elements",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
