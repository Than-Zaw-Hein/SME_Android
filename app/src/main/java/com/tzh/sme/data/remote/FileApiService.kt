package com.tzh.sme.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FileApiService {
    @Multipart
    @POST("api/File/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): Response<FileUploadResponse>
}

// FILE_SERVER_URL is now defined in app/build.gradle.kts as BuildConfig field

data class FileUploadResponse(
    val fileName: String,
    val path: String,
    val url: String
)
