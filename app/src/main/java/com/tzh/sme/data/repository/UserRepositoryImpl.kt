package com.tzh.sme.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tzh.sme.domain.repository.User
import com.tzh.sme.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override fun getUserById(userId: String): Flow<User?> = callbackFlow {
        val subscription = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = User(
                    id = snapshot.id,
                    email = snapshot.getString("email") ?: "",
                    displayName = snapshot.getString("name"),
                    phone = snapshot.getString("phone"),
                    address = snapshot.getString("address")
                )
                trySend(user)
            } else {
                trySend(null)
            }
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateProfile(user: User): Result<Unit> {
        return try {
            val userData = hashMapOf(
                "name" to user.displayName,
                "phone" to user.phone,
                "address" to user.address,
                "email" to user.email
            )
            usersCollection.document(user.id).set(userData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
