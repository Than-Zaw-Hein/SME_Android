package com.tzh.sme.di

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.tzh.sme.BuildConfig
import com.tzh.sme.BuildConfig.FILE_SERVER_URL
import com.tzh.sme.data.remote.FileApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseStore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            // Local cache size (default is 100 MB)
            setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100 * 1024 * 1024) // 100 MB
                    .build()
            )
        }
        firestore.firestoreSettings = settings
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth() = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        val storage = FirebaseStorage.getInstance()
        // Set max retry time to 60 seconds. This ensures that if a session is terminated
        // or gets stuck, it has enough time to recover, but still fails fast enough 
        // for our repository's retry logic to kick in if it's a terminal error.
        storage.maxUploadRetryTimeMillis = 60000
        storage.maxOperationRetryTimeMillis = 60000
        return storage
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("sme_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingLevel = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = loggingLevel })
            .build()
    }

    @Provides
    @Singleton
    fun provideFileApiService(okHttpClient: OkHttpClient): FileApiService {

        return Retrofit.Builder()
            .baseUrl(BuildConfig.FILE_SERVER_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FileApiService::class.java)
    }
}