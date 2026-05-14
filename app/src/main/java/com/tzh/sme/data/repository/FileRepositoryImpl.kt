package com.tzh.sme.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import com.tzh.sme.data.remote.FileApiService
import com.tzh.sme.domain.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val apiService: FileApiService,
    @ApplicationContext private val context: Context
) : FileRepository {

    override suspend fun uploadProductImage(
        uri: Uri,
        productId: String,
        fileName: String
    ): Result<String> {
        return try {
            val processedBytes =
                processImage(uri) ?: return Result.failure(Exception("Image processing failed"))

            val requestBody = processedBytes.toRequestBody("image/webp".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("file", fileName, requestBody)

            val response = apiService.uploadFile(multipartBody)

            if (response.isSuccessful && response.body() != null) {
                val serverUrl = response.body()!!.path
                Result.success(serverUrl)
            } else {
                Result.failure(Exception("Upload failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProductImage(path: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun deleteProductImagesFolder(productId: String): Result<Unit> {
        return Result.success(Unit)
    }

    private fun processImage(uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // 1. Resize: Max width 1080px while maintaining aspect ratio
            val maxWidth = 1080
            val targetWidth: Int
            val targetHeight: Int

            if (originalBitmap.width > maxWidth) {
                targetWidth = maxWidth
                targetHeight =
                    (originalBitmap.height * (maxWidth.toFloat() / originalBitmap.width)).toInt()
            } else {
                targetWidth = originalBitmap.width
                targetHeight = originalBitmap.height
            }

            val scaledBitmap =
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

            // 2. Compress to WebP (Lossy compression for best size/quality balance)
            val outputStream = ByteArrayOutputStream()
            val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }

            // Quality 80 is the "sweet spot" for WebP to get ~150KB-300KB
            scaledBitmap.compress(compressFormat, 80, outputStream)

            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
