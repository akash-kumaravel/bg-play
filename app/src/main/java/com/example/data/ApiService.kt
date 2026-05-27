package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class RemoveBgResponse(
    val image: String,
    val mask: String,
    val model: String?
)

class ApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val responseAdapter = moshi.adapter(RemoveBgResponse::class.java)

    /**
     * Posts multi-part form-data with the image file to the background removal API endpoint.
     * Returns the response with base64 encoded cutout (image) and mask (mask).
     */
    suspend fun removeBackground(
        imageFile: File,
        modelName: String = "isnet-general-use",
        x: Int? = null,
        y: Int? = null
    ): RemoveBgResponse = withContext(Dispatchers.IO) {
        val fileBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        
        val multipartBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", imageFile.name, fileBody)
            .addFormDataPart("model", modelName)
        
        x?.let { multipartBuilder.addFormDataPart("x", it.toString()) }
        y?.let { multipartBuilder.addFormDataPart("y", it.toString()) }

        val requestBody = multipartBuilder.build()

        val request = Request.Builder()
            .url("https://akkkkkeeeeee-sakyy.hf.space/api/remove-bg")
            .post(requestBody)
            .build()

        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            val errMessage = "API error (HTTP ${response.code})"
                            if (continuation.isActive) {
                                continuation.resumeWithException(IOException(errMessage))
                            }
                            return
                        }
                        val responseStr = response.body?.string()
                        if (responseStr == null) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(IOException("Response body is empty"))
                            }
                            return
                        }
                        val result = responseAdapter.fromJson(responseStr)
                        if (result != null) {
                            if (continuation.isActive) {
                                continuation.resume(result)
                            }
                        } else {
                            if (continuation.isActive) {
                                continuation.resumeWithException(IOException("Response JSON parsing returned null"))
                            }
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    } finally {
                        response.close()
                    }
                }
            })
        }
    }
}
