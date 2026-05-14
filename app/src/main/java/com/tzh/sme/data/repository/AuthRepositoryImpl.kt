package com.tzh.sme.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefs: SharedPreferences,
    private val gson: Gson
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    companion object {
        private const val KEY_USER_DATA = "user_data"
    }

    init {
        // First try to load from local storage for faster UI response
        loadUserLocally()

        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            Log.d("AuthRepository", "AuthStateListener: user is ${firebaseUser?.uid}")
            if (firebaseUser != null) {
                // Fetch additional user data from Firestore to keep it fresh
                firestore.collection("users").document(firebaseUser.uid).get()
                    .addOnSuccessListener { document ->
                        val user = User(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = document.getString("name") ?: firebaseUser.displayName,
                            phone = document.getString("phone"),
                            address = document.getString("address")
                        )

                        updateUser(user)
                    }
                    .addOnFailureListener {
                        Log.e("AuthRepository", "Firestore fetch failed: ${it.message}")
                        // If firestore fails, we still have firebaseUser, 
                        // use existing local data or create basic user
                        if (_currentUser.value == null) {
                            val user = User(
                                id = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                displayName = firebaseUser.displayName
                            )
                            updateUser(user)
                        } else {
                            _isInitialized.value = true
                        }
                    }
            } else {
                clearLocalUser()
                _isInitialized.value = true
            }
        }
    }

    private fun loadUserLocally() {
        val json = prefs.getString(KEY_USER_DATA, null)
        if (json != null) {
            try {
                val user = gson.fromJson(json, User::class.java)
                _currentUser.value = user
                _isInitialized.value = true
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error parsing local user data", e)
            }
        }
    }

    private fun updateUser(user: User) {
        _currentUser.value = user
        val json = gson.toJson(user)
        prefs.edit().putString(KEY_USER_DATA, json).apply()
        _isInitialized.value = true
    }

    private fun clearLocalUser() {
        _currentUser.value = null
        prefs.edit().remove(KEY_USER_DATA).apply()
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            Result.success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            Result.failure(Exception("No account found with this email."))
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Incorrect password."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(
        name: String,
        phone: String,
        address: String,
        email: String,
        password: String
    ): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("User creation failed")

            val userData = hashMapOf(
                "name" to name,
                "phone" to phone,
                "address" to address,
                "email" to email
            )

            firestore.collection("users").document(userId).set(userData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Google Sign-In failed")

            // Check if user exists in Firestore, if not create basic entry
            val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
            if (!doc.exists()) {
                val userData = hashMapOf(
                    "name" to (firebaseUser.displayName ?: ""),
                    "email" to (firebaseUser.email ?: ""),
                    "phone" to (firebaseUser.phoneNumber ?: ""),
                    "address" to ""
                )
                firestore.collection("users").document(firebaseUser.uid).set(userData).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        clearLocalUser()
    }

    override suspend fun sendPasswordResetEmail(email: String?): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email ?: "").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendVerificationCode(email: String): Result<Unit> {
        // In a real app, you would call your backend to send a code via email
        // For simulation, we'll just succeed
        return try {
            Log.d("AuthRepository", "Sending verification code to $email")
            // Simulate API call
            kotlinx.coroutines.delay(1000)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyCode(email: String, code: String): Result<Unit> {
        // In a real app, you would verify the code with your backend
        // For simulation, we'll accept '123456'
        return try {
            Log.d("AuthRepository", "Verifying code $code for $email")
            kotlinx.coroutines.delay(1000)
            if (code == "123456") {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid verification code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isEmailRegistered(email: String): Result<Boolean> {
        return try {
            val result = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()
            Result.success(!result.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(oldPass: String, newPass: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            val email = user.email ?: throw Exception("User email not found")
            
            // Re-authenticate user to verify old password
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPass)
            user.reauthenticate(credential).await()
            
            // Update to new password
            user.updatePassword(newPass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
