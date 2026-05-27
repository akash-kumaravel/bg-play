package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class BgType {
    TRANSPARENT, SOLID, GRADIENT, CUSTOM_IMAGE, BLUR
}

enum class BulkStatus {
    WAITING, PROCESSING, SUCCESS, FAILED
}

data class BulkItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    var status: BulkStatus = BulkStatus.WAITING,
    var progress: Float = 0f,
    var originalPath: String? = null,
    var processedPath: String? = null,
    var error: String? = null
)

class RemBgViewModel(private val repository: ProjectRepository) : ViewModel() {

    private val apiService = ApiService()

    // Room Database active flow
    val projectsState: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Single active project states
    val activeProject = MutableStateFlow<ProjectEntity?>(null)
    val originalBitmap = MutableStateFlow<Bitmap?>(null)
    val cutoutBitmap = MutableStateFlow<Bitmap?>(null) // Cutout foreground
    val maskBitmap = MutableStateFlow<Bitmap?>(null) // Mask from api
    
    val editorLoading = MutableStateFlow(false)
    val editorError = MutableStateFlow<String?>(null)

    // Bottom editor tool sheets
    val activeTool = MutableStateFlow<String?>("remove_bg") // e.g. "remove_bg", "crop", "background", "blur", "shadow", "export"

    // Background editor parameters
    val backgroundType = MutableStateFlow(BgType.TRANSPARENT)
    val selectedSolidColor = MutableStateFlow(Color.White)
    val selectedGradientColors = MutableStateFlow(listOf(Color.Cyan, Color.Blue))
    val blurBackgroundAmount = MutableStateFlow(15f) // dp or scale value
    val customBgPath = MutableStateFlow<String?>(null)
    val customBgBitmap = MutableStateFlow<Bitmap?>(null)

    // Shadow & Canvas adjustments
    val shadowEnabled = MutableStateFlow(false)
    val shadowOffsetX = MutableStateFlow(12f)
    val shadowOffsetY = MutableStateFlow(12f)
    val shadowRadius = MutableStateFlow(15f)
    val shadowColor = MutableStateFlow(Color.Black)

    // Bulk remove states
    val bulkQueue = MutableStateFlow<List<BulkItem>>(emptyList())
    val isBulkPaused = MutableStateFlow(false)
    val isBulkRunning = MutableStateFlow(false)

    /**
     * Creates or loads a project entity from selected URI
     */
    fun createProjectFromUri(context: Context, uri: Uri, initialTool: String = "remove_bg", onReady: () -> Unit) {
        viewModelScope.launch {
            editorLoading.value = true
            editorError.value = null
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Failed to open stream")
                // Save original to cache files dir
                val tempPath = repository.saveUriToFile(inputStream, "orig")
                
                // Load original bitmap
                val loadedBmp = ProjectRepository.loadBitmap(tempPath)
                if (loadedBmp == null) {
                    throw Exception("Failed to decode image data")
                }
                
                // Create database project
                val cleanFileName = "Project_${System.currentTimeMillis() % 10000}"
                val project = ProjectEntity(
                    name = cleanFileName,
                    originalImagePath = tempPath
                )
                val newId = repository.insert(project)
                val savedProject = project.copy(id = newId.toInt())
                
                // Set as active
                activeProject.value = savedProject
                originalBitmap.value = loadedBmp
                cutoutBitmap.value = null
                maskBitmap.value = null
                activeTool.value = initialTool
                
                // Reset edit parameters
                backgroundType.value = BgType.TRANSPARENT
                shadowEnabled.value = false
                customBgPath.value = null
                customBgBitmap.value = null
                
                onReady()
            } catch (e: Exception) {
                editorError.value = e.message ?: "Failed to import image"
            } finally {
                editorLoading.value = false
            }
        }
    }

    /**
     * Loads an existing project into the editor
     */
    fun loadProject(project: ProjectEntity) {
        viewModelScope.launch {
            editorLoading.value = true
            editorError.value = null
            activeProject.value = project
            activeTool.value = "remove_bg"
            
            try {
                val orig = ProjectRepository.loadBitmap(project.originalImagePath)
                originalBitmap.value = orig
                
                val cutout = project.processedImagePath?.let { ProjectRepository.loadBitmap(it) }
                cutoutBitmap.value = cutout
                
                val mask = project.maskImagePath?.let { ProjectRepository.loadBitmap(it) }
                maskBitmap.value = mask
                
                // Reset parameters
                backgroundType.value = if (cutout != null) BgType.TRANSPARENT else BgType.TRANSPARENT
                shadowEnabled.value = false
                customBgPath.value = null
                customBgBitmap.value = null
            } catch (e: Exception) {
                editorError.value = "Failed to load project files"
            } finally {
                editorLoading.value = false
            }
        }
    }

    /**
     * Executes AI Background removal on the current active project
     */
    fun processBackgroundRemoval() {
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            editorLoading.value = true
            editorError.value = null
            try {
                val file = File(proj.originalImagePath)
                // Call API
                val response = apiService.removeBackground(file)
                
                // Decode background result and mask
                val processedPath = repository.saveBase64ToFile(response.image, "cutout")
                val maskPath = repository.saveBase64ToFile(response.mask, "mask")
                
                // Update active project details in database
                val updatedProj = proj.copy(
                    processedImagePath = processedPath,
                    maskImagePath = maskPath
                )
                repository.update(updatedProj)
                
                // Refresh states
                activeProject.value = updatedProj
                cutoutBitmap.value = ProjectRepository.loadBitmap(processedPath)
                maskBitmap.value = ProjectRepository.loadBitmap(maskPath)
                backgroundType.value = BgType.TRANSPARENT
            } catch (e: Exception) {
                e.printStackTrace()
                editorError.value = "AI API processing failed: ${e.localizedMessage ?: "Network issue"}"
            } finally {
                editorLoading.value = false
            }
        }
    }

    /**
     * Sets crop configuration aspect ratio and applies crop bounds locally to originalBitmap/cutoutBitmap
     */
    fun applyLocalCrop(context: Context, rect: RectF) {
        val currentOrig = originalBitmap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            editorLoading.value = true
            try {
                val width = currentOrig.width
                val height = currentOrig.height
                
                val left = (rect.left * width).toInt().coerceIn(0, width - 1)
                val top = (rect.top * height).toInt().coerceIn(0, height - 1)
                val right = (rect.right * width).toInt().coerceIn(left + 1, width)
                val bottom = (rect.bottom * height).toInt().coerceIn(top + 1, height)
                
                val croppedOrig = Bitmap.createBitmap(currentOrig, left, top, right - left, bottom - top)
                
                // If cutout is available, apply same proportional crop bounds
                var croppedCutout: Bitmap? = null
                val currentCutout = cutoutBitmap.value
                if (currentCutout != null) {
                    val scaleX = currentCutout.width.toFloat() / width
                    val scaleY = currentCutout.height.toFloat() / height
                    val cLeft = (left * scaleX).toInt().coerceIn(0, currentCutout.width - 1)
                    val cTop = (top * scaleY).toInt().coerceIn(0, currentCutout.height - 1)
                    val cRight = (right * scaleX).toInt().coerceIn(cLeft + 1, currentCutout.width)
                    val cBottom = (bottom * scaleY).toInt().coerceIn(cTop + 1, currentCutout.height)
                    
                    croppedCutout = Bitmap.createBitmap(currentCutout, cLeft, cTop, cRight - cLeft, cBottom - cTop)
                }
                
                // Save original to files
                val newModelPath = repository.saveBitmapToFile(croppedOrig, "orig_cropped")
                val newProcessedPath = croppedCutout?.let { repository.saveBitmapToFile(it, "cutout_cropped") }
                
                val activeProj = activeProject.value
                if (activeProj != null) {
                    val updated = activeProj.copy(
                        originalImagePath = newModelPath,
                        processedImagePath = newProcessedPath ?: activeProj.processedImagePath
                    )
                    repository.update(updated)
                    activeProject.value = updated
                }
                
                withContext(Dispatchers.Main) {
                    originalBitmap.value = croppedOrig
                    if (croppedCutout != null) {
                        cutoutBitmap.value = croppedCutout
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    editorError.value = "Failed to crop image: ${e.message}"
                }
            } finally {
                editorLoading.value = false
            }
        }
    }

    /**
     * Custom Custom Background image picker resolution
     */
    fun selectCustomBgUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val fileLoc = repository.saveUriToFile(inputStream, "custom_bg")
                customBgPath.value = fileLoc
                customBgBitmap.value = ProjectRepository.loadBitmap(fileLoc)
                backgroundType.value = BgType.CUSTOM_IMAGE
            } catch (e: Exception) {
                editorError.value = "Failed to load custom background image"
            }
        }
    }

    /**
     * Deletes a project
     */
    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteById(project.id)
            if (activeProject.value?.id == project.id) {
                activeProject.value = null
                originalBitmap.value = null
                cutoutBitmap.value = null
                maskBitmap.value = null
            }
        }
    }

    /**
     * Bulk processing operations
     */
    fun addImagesToBulkQueue(context: Context, uris: List<Uri>) {
        val currentList = bulkQueue.value.toMutableList()
        uris.forEach { uri ->
            val name = "Bulk_${System.currentTimeMillis() % 10000}_${currentList.size + 1}.jpg"
            currentList.add(BulkItem(uri = uri, name = name))
        }
        bulkQueue.value = currentList
    }

    fun removeBulkItem(item: BulkItem) {
        bulkQueue.value = bulkQueue.value.filter { it.id != item.id }
    }

    fun clearBulkQueue() {
        bulkQueue.value = emptyList()
        isBulkRunning.value = false
    }

    fun setBulkPaused(paused: Boolean) {
        isBulkPaused.value = paused
    }

    fun startBulkProcessing(context: Context) {
        if (isBulkRunning.value) return
        isBulkRunning.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            val queue = bulkQueue.value
            
            for (index in queue.indices) {
                // If user cleared queue while running
                if (!isBulkRunning.value) break
                
                // If queue is paused, wait until resume
                while (isBulkPaused.value) {
                    kotlinx.coroutines.delay(1000)
                    if (!isBulkRunning.value) break
                }
                
                val item = bulkQueue.value.getOrNull(index) ?: continue
                if (item.status == BulkStatus.SUCCESS || item.status == BulkStatus.PROCESSING) continue
                
                // Update state to processing
                updateBulkItemStatus(item.id, BulkStatus.PROCESSING, 0.1f)
                
                try {
                    val stream = context.contentResolver.openInputStream(item.uri) ?: throw Exception("Stream unavailable")
                    val origPath = repository.saveUriToFile(stream, "bulk_orig")
                    item.originalPath = origPath
                    
                    updateBulkItemStatus(item.id, BulkStatus.PROCESSING, 0.4f, originalPath = origPath)
                    
                    // Call API using temp file saved
                    val res = apiService.removeBackground(File(origPath))
                    
                    updateBulkItemStatus(item.id, BulkStatus.PROCESSING, 0.8f, originalPath = origPath)
                    
                    val processedPath = repository.saveBase64ToFile(res.image, "bulk_cutout")
                    val maskPath = repository.saveBase64ToFile(res.mask, "bulk_mask")
                    
                    item.processedPath = processedPath
                    
                    // Auto-insert file into database list so it appears in recent projects!
                    val proj = ProjectEntity(
                        name = item.name,
                        originalImagePath = origPath,
                        processedImagePath = processedPath,
                        maskImagePath = maskPath
                    )
                    repository.insert(proj)
                    
                    updateBulkItemStatus(item.id, BulkStatus.SUCCESS, 1.0f, originalPath = origPath, processedPath = processedPath)
                } catch (e: Exception) {
                    e.printStackTrace()
                    updateBulkItemStatus(item.id, BulkStatus.FAILED, 1.0f, error = e.localizedMessage ?: "API error")
                }
            }
            
            isBulkRunning.value = false
        }
    }

    private fun updateBulkItemStatus(
        id: String,
        status: BulkStatus,
        progress: Float,
        originalPath: String? = null,
        processedPath: String? = null,
        error: String? = null
    ) {
        val updated = bulkQueue.value.map { item ->
            if (item.id == id) {
                item.copy(
                    status = status,
                    progress = progress,
                    originalPath = originalPath ?: item.originalPath,
                    processedPath = processedPath ?: item.processedPath,
                    error = error
                )
            } else {
                item
            }
        }
        bulkQueue.value = updated
    }

    /**
     * Saves a successfully processed bulk item to the device gallery using MediaStore.
     */
    fun saveBulkItemToGallery(context: Context, item: BulkItem, onSuccess: () -> Unit = {}) {
        val processedPath = item.processedPath ?: return
        viewModelScope.launch {
            val isSuccess = withContext(Dispatchers.IO) {
                try {
                    val file = File(processedPath)
                    if (!file.exists()) return@withContext false
                    
                    val resolver = context.contentResolver
                    val filename = "BGWrap_${System.currentTimeMillis()}_${item.id.take(4)}.png"
                    val mimeType = "image/png"
                    
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/BGWrap")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }
                    
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    var writeSuccess = false
                    if (uri != null) {
                        resolver.openOutputStream(uri).use { out ->
                            if (out != null) {
                                file.inputStream().use { input ->
                                    input.copyTo(out)
                                    writeSuccess = true
                                }
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val updateValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.IS_PENDING, 0)
                            }
                            resolver.update(uri, updateValues, null, null)
                        }
                    }
                    writeSuccess
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            if (isSuccess) {
                onSuccess()
            }
        }
    }

    /**
     * Saves all successfully processed bulk items to the device gallery in a single click.
     */
    fun saveAllBulkItemsToGallery(context: Context, onComplete: (count: Int) -> Unit) {
        viewModelScope.launch {
            val successItems = bulkQueue.value.filter { it.status == BulkStatus.SUCCESS && it.processedPath != null }
            if (successItems.isEmpty()) {
                onComplete(0)
                return@launch
            }
            
            val savedCount = withContext(Dispatchers.IO) {
                var count = 0
                for (item in successItems) {
                    val file = File(item.processedPath!!)
                    if (!file.exists()) continue
                    
                    try {
                        val resolver = context.contentResolver
                        val filename = "BGWrap_${System.currentTimeMillis()}_${item.id.take(4)}.png"
                        val mimeType = "image/png"
                        
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/BGWrap")
                                put(MediaStore.MediaColumns.IS_PENDING, 1)
                            }
                        }
                        
                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            resolver.openOutputStream(uri).use { out ->
                                if (out != null) {
                                    file.inputStream().use { input ->
                                        input.copyTo(out)
                                    }
                                }
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val updateValues = ContentValues().apply {
                                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                                }
                                resolver.update(uri, updateValues, null, null)
                            }
                            count++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                count
            }
            onComplete(savedCount)
        }
    }
}

class RemBgViewModelFactory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RemBgViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RemBgViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
