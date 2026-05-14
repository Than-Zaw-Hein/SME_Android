package com.tzh.sme.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.tzh.sme.domain.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : FileRepository {

    override suspend fun uploadProductImage(uri: Uri, productId: String, fileName: String): Result<String> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        val processedBytes = try {
            processImage(uri) ?: return Result.failure(Exception("Image processing failed"))
        } catch (e: Exception) {
            return Result.failure(e)
        }

        // Updated path to match: users/{userId}/products/{productId}/{imageName}
        val storageRef = storage.reference.child("users/$userId/products/$productId/$fileName")
        val metadata = StorageMetadata.Builder()
            .setContentType("image/webp")
            .build()

        var lastException: Exception? = null
        for (attempt in 1..3) {
            try {
                storageRef.putBytes(processedBytes, metadata).await()
                val downloadUrl = storageRef.downloadUrl.await()
                return Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                lastException = e
                if (attempt < 3) {
                    delay(1000L * attempt) // Exponential backoff
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("Upload failed after 3 attempts"))
    }

    override suspend fun deleteProductImage(path: String): Result<Unit> {
        return try {
            // path might be a full URL or a relative path
            val storageRef = if (path.startsWith("http")) {
                storage.getReferenceFromUrl(path)
            } else {
                storage.reference.child(path)
            }
            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProductImagesFolder(productId: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val folderRef = storage.reference.child("users/$userId/products/$productId")
            val listResult = folderRef.listAll().await()
            listResult.items.forEach { it.delete().await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun processImage(uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val maxWidth = 1080
            val targetWidth: Int
            val targetHeight: Int
            
            if (originalBitmap.width > maxWidth) {
                targetWidth = maxWidth
                targetHeight = (originalBitmap.height * (maxWidth.toFloat() / originalBitmap.width)).toInt()
            } else {
                targetWidth = originalBitmap.width
                targetHeight = originalBitmap.height
            }

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

            val outputStream = ByteArrayOutputStream()
            val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            
            scaledBitmap.compress(compressFormat, 80, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
