package com.tzh.sme.data.repository

import com.tzh.sme.data.model.ShopModel
import com.google.firebase.firestore.FirebaseFirestore
import com.tzh.sme.domain.repository.User
import com.tzh.sme.domain.repository.UserRepository
import com.tzh.sme.domain.repository.UserRole
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
                val roleStr = snapshot.getString("role") ?: "ADMIN"
                val user = User(
                    id = snapshot.id,
                    email = snapshot.getString("email") ?: "",
                    displayName = snapshot.getString("name"),
                    phone = snapshot.getString("phone"),
                    shopId = snapshot.getString("shopId"),
                    role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.ADMIN }
                )
                trySend(user)
            } else {
                trySend(null)
            }
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateProfile(user: User,shopName: String): Result<Unit> {
        return try {
            val userData = hashMapOf(
                "name" to user.displayName,
                "phone" to user.phone,
                "email" to user.email,
                "shopId" to user.shopId,
                "shopName" to shopName,
                "role" to user.role.name
            )
            usersCollection.document(user.id).set(userData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getShopById(shopId: String): Flow<ShopModel?> = callbackFlow {
        val subscription = firestore.collection("shops").document(shopId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val shop = snapshot.toObject(ShopModel::class.java)
                    trySend(shop)
                } else {
                    trySend(null)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateShop(shop: ShopModel): Result<Unit> {
        return try {
            val batch = firestore.batch()
            
            // 1. Update the Shop document
            val shopRef = firestore.collection("shops").document(shop.id)
            batch.set(shopRef, shop)

            // 2. Find all users belonging to this shop and update their cached shopName
            val usersInShop = firestore.collection("users")
                .whereEqualTo("shopId", shop.id)
                .get()
                .await()

            for (userDoc in usersInShop.documents) {
                batch.update(userDoc.reference, "shopName", shop.name)
            }

            // 3. Commit atomically
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
