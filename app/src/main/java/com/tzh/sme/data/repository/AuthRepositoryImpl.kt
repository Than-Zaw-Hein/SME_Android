package com.tzh.sme.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.User
import com.tzh.sme.domain.repository.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefs: SharedPreferences,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var userDocumentListener: ListenerRegistration? = null

    companion object {
        private const val KEY_USER_DATA = "user_data"
    }

    init {
        // First try to load from local storage for faster UI response
        loadUserLocally()

        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            Log.d("AuthRepository", "AuthStateListener: user is ${firebaseUser?.uid}")

            userDocumentListener?.remove()

            if (firebaseUser != null) {
                // Listen to user document for live updates (e.g., role changes by Admin)
                userDocumentListener = firestore.collection("users").document(firebaseUser.uid)
                    .addSnapshotListener { document, error ->
                        if (error != null) {
                            Log.e("AuthRepository", "Firestore listener failed: ${error.message}")
                            _isInitialized.value = true
                            return@addSnapshotListener
                        }

                        if (document != null && document.exists()) {
                            val roleStr = document.getString("role") ?: "ADMIN"
                            val newRole = try {
                                UserRole.valueOf(roleStr)
                            } catch (_: Exception) {
                                UserRole.ADMIN
                            }

                            val previousUser = _currentUser.value
                            // Update user data and local cache when changes occur (including role)
                            val user = User(
                                id = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                displayName = document.getString("name")
                                    ?: firebaseUser.displayName,
                                phone = document.getString("phone"),
                                shopId = document.getString("shopId") ?: firebaseUser.uid,
                                shopName = document.getString("shopName") ?: "SME Business",
                                role = newRole,
                                isEmailVerified = firebaseUser.isEmailVerified
                            )

                            if (previousUser != user) {
                                Log.d("AuthRepository", "User data updated: role is now $newRole")
                                updateUser(user)
                            }
                        } else {
                            Log.e("AuthRepository", "User document not found or deleted")
                            if (_currentUser.value != null) {
                                auth.signOut()
                            }
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
            Log.e("AuthRepository", "SignIn failed: User not found [${e.errorCode}]", e)
            Result.failure(Exception("No account found with this email."))
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Log.e("AuthRepository", "SignIn failed: Invalid credentials [${e.errorCode}]", e)
            Result.failure(Exception("Incorrect password."))
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            Log.e("AuthRepository", "Auth error: ${e.errorCode} - ${e.message}", e)
            val message = when {
                e.message?.contains("RecaptchaAction") == true ->
                    "Security check failed (Recaptcha). Please ensure your SHA-256 is registered in Firebase Console and Play Integrity is enabled."

                else -> e.message ?: "Authentication failed"
            }
            Result.failure(Exception(message))
        } catch (e: com.google.firebase.FirebaseException) {
            Log.e("AuthRepository", "Firebase error: ${e.message}", e)
            val message = when {
                e.message?.contains("RecaptchaAction") == true ->
                    "Security check failed (Recaptcha). This usually happens due to missing SHA-256 in Firebase Console or App Check misconfiguration."

                else -> e.message ?: "Authentication failed"
            }
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Log.e("AuthRepository", "SignIn failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(
        name: String,
        phone: String,
        address: String,
        email: String,
        password: String,
        shopName: String,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("User creation failed")
            result.user?.sendEmailVerification()
            val batch = firestore.batch()

            // 1. Prepare Shop document
            val shopRef = firestore.collection("shops").document()
            val shopId = shopRef.id
            val shopData = hashMapOf(
                "id" to shopId,
                "name" to shopName,
                "ownerId" to userId,
                "address" to address,
                "latitude" to latitude,
                "longitude" to longitude,
                "createdAt" to System.currentTimeMillis()
            )
            batch.set(shopRef, shopData)

            // 2. Prepare User document linked to the Shop (Personal data only)
            val userRef = firestore.collection("users").document(userId)
            val userData = hashMapOf(
                "id" to userId,
                "name" to name,
                "phone" to phone,
                "email" to email,
                "shopId" to shopId,
                "shopName" to shopName,
                "role" to UserRole.ADMIN.name
            )
            batch.set(userRef, userData)

            // 3. Commit both writes atomically
            batch.commit().await()
            Log.d("AuthRepository", "Signup successful: Shop $shopId and User $userId created")

            Result.success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            Log.e("AuthRepository", "Signup failed: Email already in use", e)
            Result.failure(Exception("This email address is already registered. Please login or use a different email."))
        } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            Log.e("AuthRepository", "Signup failed: Weak password", e)
            Result.failure(Exception("The password is too weak. Please use at least 6 characters."))
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Log.e("AuthRepository", "Signup failed: Invalid email", e)
            Result.failure(Exception("The email address is badly formatted."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Signup failed: ${e.message}", e)
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
                    "phone" to (firebaseUser.phoneNumber ?: "")
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

    override suspend fun changePassword(oldPass: String, newPass: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            val email = user.email ?: throw Exception("User email not found")

            // Re-authenticate user to verify old password
            val credential =
                com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPass)
            user.reauthenticate(credential).await()

            // Update to new password
            user.updatePassword(newPass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reloadUser(): Result<Unit> {
        return try {
            auth.currentUser?.reload()?.await()
            // After reload, we need to manually trigger a refresh of our local user state
            // because AuthStateListener might not fire for reload() if the user identity didn't change
            auth.currentUser?.let { firebaseUser ->
                val currentUserVal = _currentUser.value
                if (currentUserVal != null) {
                    _currentUser.value =
                        currentUserVal.copy(isEmailVerified = firebaseUser.isEmailVerified)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getShopStaff(shopId: String): Flow<List<User>> = callbackFlow {
        val subscription = firestore.collection("users")
            .whereEqualTo("shopId", shopId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val staff = snapshot.documents.mapNotNull { doc ->
                        val roleStr = doc.getString("role") ?: "SELLER"
                        User(
                            id = doc.id,
                            email = doc.getString("email") ?: "",
                            displayName = doc.getString("name"),
                            phone = doc.getString("phone"),
                            shopId = doc.getString("shopId"),
                            shopName = doc.getString("shopName"),
                            role = try {
                                UserRole.valueOf(roleStr)
                            } catch (_: Exception) {
                                UserRole.SELLER
                            }
                        )
                    }
                    trySend(staff)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createStaffAccount(
        name: String,
        email: String,
        pass: String,
        phone: String,
        role: UserRole
    ): Result<Unit> {
        val currentAdmin =
            currentUser.value ?: return Result.failure(Exception("Not authenticated as Admin"))
        val shopId = currentAdmin.shopId ?: return Result.failure(Exception("Shop not found"))
        val shopName = currentAdmin.shopName ?: "SME Business"

        return withContext(Dispatchers.IO) {
            try {
                // Use a secondary Firebase App to create the user without logging out the Admin
                val secondaryAppName = "SecondaryApp_${System.currentTimeMillis()}"
                val options = auth.app.options

                val secondaryApp = FirebaseApp.initializeApp(context, options, secondaryAppName)
                val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

                try {
                    val result = secondaryAuth.createUserWithEmailAndPassword(email, pass).await()
                    val newStaffId = result.user?.uid ?: throw Exception("Staff creation failed")
                    result.user?.sendEmailVerification()
                    val userData = hashMapOf(
                        "id" to newStaffId,
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "shopId" to shopId,
                        "shopName" to shopName,
                        "role" to role.name
                    )

                    firestore.collection("users").document(newStaffId).set(userData).await()

                    Result.success(Unit)
                } finally {
                    secondaryApp.delete()
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Staff creation failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateStaffAccount(
        userId: String,
        name: String,
        phone: String,
        role: UserRole
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(userId).update(
                    mapOf(
                        "name" to name,
                        "role" to role.name,
                        "phone" to phone
                    )
                ).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Staff update failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun removeStaff(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Remove from Firestore first (this revokes shop access)
                firestore.collection("users").document(userId).delete().await()

                // Note: Complete deletion from Firebase Auth from a client device 
                // typically requires Firebase Admin SDK (Server-side).
                // On the client, we have removed their 'shopId' link and user record.
                // The account remains in Auth but is 'orphaned' and cannot access any data
                // because our Firestore Rules require a valid user document.

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Staff removal failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}
