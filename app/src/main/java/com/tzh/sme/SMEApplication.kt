package com.tzh.sme

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.Logger
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SMEApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        // Initialize ProviderInstaller to ensure secure communication and fix GMS issues
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (e: Exception) {
            Log.e("SMEApplication", "Google Play Services security provider installation failed", e)
        }

        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = Firebase.appCheck

        if (BuildConfig.DEBUG) {
            // Install the debug provider before fetching any tokens
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d("SMEApplication", "Firebase App Check Debug Provider installed")
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }.logger(object : Logger {
                override var level: Int = Log.ERROR

                override fun log(
                    tag: String,
                    priority: Int,
                    message: String?,
                    throwable: Throwable?
                ) {
                    Log.e("image Loader Log", tag)
                }

            })
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}
