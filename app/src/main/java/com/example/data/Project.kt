package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val originalImagePath: String,
    val processedImagePath: String? = null,
    val maskImagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aura_bg_remover_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ProjectRepository(private val projectDao: ProjectDao, private val context: Context) {

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Int): ProjectEntity? = withContext(Dispatchers.IO) {
        projectDao.getProjectById(id)
    }

    suspend fun insert(project: ProjectEntity): Long = withContext(Dispatchers.IO) {
        projectDao.insertProject(project)
    }

    suspend fun update(project: ProjectEntity) = withContext(Dispatchers.IO) {
        projectDao.updateProject(project)
    }

    suspend fun deleteById(id: Int) = withContext(Dispatchers.IO) {
        // First delete local files if possible to keep user storage clean
        val project = projectDao.getProjectById(id)
        if (project != null) {
            try {
                File(project.originalImagePath).delete()
                project.processedImagePath?.let { File(it).delete() }
                project.maskImagePath?.let { File(it).delete() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        projectDao.deleteProjectById(id)
    }

    /**
     * Saves a base64 string directly into a PNG file inside internal storage
     */
    suspend fun saveBase64ToFile(base64Str: String, prefix: String): String = withContext(Dispatchers.IO) {
        val cleanBase64 = base64Str.replace("\n", "").replace("\r", "").trim()
        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        val file = File(context.filesDir, "project_${prefix}_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out ->
            out.write(decodedBytes)
        }
        file.absolutePath
    }

    /**
     * Saves a bitmap image into a file in private storage
     */
    suspend fun saveBitmapToFile(bitmap: Bitmap, prefix: String): String = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "project_${prefix}_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file.absolutePath
    }

    /**
     * Saves a URI stream directly to internal storage and returns the offline file path
     */
    suspend fun saveUriToFile(inputStream: InputStream, prefix: String): String = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "project_${prefix}_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            inputStream.copyTo(out)
        }
        file.absolutePath
    }

    companion object {
        fun loadBitmap(path: String): Bitmap? {
            return try {
                BitmapFactory.decodeFile(path)
            } catch (e: Exception) {
                null
            }
        }
    }
}
