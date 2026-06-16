package com.tzh.sme.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.tzh.sme.domain.model.SystemImage
import com.tzh.sme.domain.repository.AuthRepository
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
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : FileRepository {

    override suspend fun uploadProductImage(
        uri: Uri,
        productId: String,
        productName: String,
        shopName: String,
        fileName: String
    ): Result<String> {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return Result.failure(Exception("Shop not found"))

        // Create readable folder names
        val shopFolderName = "${shopName}_$shopId"
        val productFolderName = "${productName}_$productId"

        // Ensure Firebase Auth is actually initialized and has the same user
        // Sometimes the local state is ahead of the Firebase SDK's internal state
        var firebaseUser = FirebaseAuth.getInstance().currentUser
        var authWaitAttempts = 0
        while (firebaseUser == null && authWaitAttempts < 10) {
            Log.d("FirebaseStorage", "Waiting for Firebase Auth... ($authWaitAttempts)")
            delay(500)
            firebaseUser = FirebaseAuth.getInstance().currentUser
            authWaitAttempts++
        }

        if (firebaseUser == null) {
            return Result.failure(Exception("Authentication required for upload. Please sign in again."))
        }

        val processedBytes = try {
            processImage(uri) ?: return Result.failure(Exception("Image processing failed"))
        } catch (e: Exception) {
            return Result.failure(e)
        }

        Log.d("FirebaseStorage", "Starting upload: $fileName (${processedBytes.size} bytes)")

        // Updated path to match: shops/{shopName_shopId}/products/{productName_productId}/{imageName}
        val storageRef = storage.reference
            .child("shops")
            .child(shopFolderName)
            .child("products")
            .child(productFolderName)
            .child(fileName)

        val metadata = StorageMetadata.Builder()
            .setContentType("image/webp")
            .build()

        var lastException: Exception? = null
        for (attempt in 1..3) {
            try {
                // If we suspect App Check or Auth token issues (e.g. after a failure), 
                // we try to force a token refresh on subsequent attempts.
                if (attempt > 1) {
                    try {
                        Log.d("FirebaseStorage", "Refreshing tokens before retry $attempt...")
                        // Force refresh tokens
                        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()
                        FirebaseAppCheck.getInstance().getToken(true).await()
                    } catch (e: Exception) {
                        Log.w("FirebaseStorage", "Token refresh failed: ${e.message}")
                    }
                }

                storageRef.putBytes(processedBytes, metadata).await()
                val downloadUrl = storageRef.downloadUrl.await()
                val urlString = downloadUrl.toString()
                
                // Register image in Firestore for global tracking (matching Web behavior)
                registerImage(urlString, storageRef.path, productId, shopId)
                
                Log.d("FirebaseStorage", "Upload successful: $fileName")
                return Result.success(urlString)
            } catch (e: Exception) {
                lastException = e
                val message = e.message ?: ""
                Log.e("FirebaseStorage", "Upload attempt $attempt failed: $message")
                
                val isAuthError = message.contains("401") || 
                                 message.contains("403") ||
                                 message.contains("App Check", ignoreCase = true) ||
                                 message.contains("terminated", ignoreCase = true) ||
                                 (e is StorageException && (
                                     e.errorCode == StorageException.ERROR_NOT_AUTHENTICATED ||
                                     e.errorCode == StorageException.ERROR_NOT_AUTHORIZED
                                 ))

                if (isAuthError) {
                    try {
                        Log.d("FirebaseStorage", "Aggressive token refresh due to auth/session error...")
                        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()
                        FirebaseAppCheck.getInstance().getToken(true).await()
                    } catch (tokenEx: Exception) {
                        Log.e("FirebaseStorage", "Aggressive token refresh failed", tokenEx)
                    }
                }

                if (attempt < 3) {
                    val waitTime = 2000L * attempt
                    Log.d("FirebaseStorage", "Waiting ${waitTime}ms before retry...")
                    delay(waitTime)
                }
            }
        }
        
        val errorMsg = lastException?.message ?: "Upload failed after 3 attempts"
        if (errorMsg.contains("App Check", ignoreCase = true)) {
            return Result.failure(Exception("Security verification failed (App Check). Please ensure you have a stable connection and try again."))
        }
        if (errorMsg.contains("terminated", ignoreCase = true)) {
            return Result.failure(Exception("The upload session was interrupted by the server. Please try again."))
        }
        return Result.failure(lastException ?: Exception(errorMsg))
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
            
            // Unregister from Firestore tracking
            unregisterImageByPath(storageRef.path)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProductImagesFolder(productId: String, productName: String, shopName: String): Result<Unit> {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return Result.failure(Exception("Shop not found"))

        val shopFolderName = "${shopName}_$shopId"
        val productFolderName = "${productName}_$productId"

        return try {
            val folderRef = storage.reference.child("shops/$shopFolderName/products/$productFolderName")
            val listResult = folderRef.listAll().await()
            listResult.items.forEach { item ->
                item.delete().await()
                unregisterImageByPath(item.path)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerImage(url: String, path: String, relatedEntityId: String, shopId: String): Result<Unit> {
        return try {
            val id = firestore.collection("images").document().id
            val systemImage = SystemImage(
                id = id,
                url = url,
                path = path,
                relatedEntityId = relatedEntityId,
                shopId = shopId,
                timestamp = System.currentTimeMillis().toDouble()
            )
            firestore.collection("images").document(id).set(systemImage).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun unregisterImageByPath(path: String) {
        try {
            val snapshot = firestore.collection("images")
                .whereEqualTo("path", path)
                .get()
                .await()
            snapshot.documents.forEach { it.reference.delete().await() }
        } catch (e: Exception) {
            Log.e("FirebaseStorage", "Failed to unregister image metadata", e)
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
