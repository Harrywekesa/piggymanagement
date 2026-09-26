package com.phms.app.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.phms.app.data.local.dao.MarketDao
import com.phms.app.data.local.entity.BuyerEntity
import kotlinx.coroutines.tasks.await

/**
 * FirestoreBuyerSync — handles the shared "all Kenya farmers" buyers directory.
 *
 * Architecture:
 *  - Firestore collection: "shared_buyers"
 *  - Each document = one buyer in the shared directory
 *  - On app launch: pulls all Firestore buyers → upserts to local Room DB (is_community = true)
 *  - When a farmer adds a buyer and taps "Share with all farmers":
 *      → saves locally + pushes to Firestore so every other farmer sees it
 *  - Works offline: local Room DB is the source of truth for the UI.
 *    Firestore sync happens in background when internet is available.
 */
object FirestoreBuyerSync {

    private val TAG = "FirestoreBuyerSync"
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val sharedBuyersCollection get() = firestore.collection("shared_buyers")

    /**
     * Pull all shared buyers from Firestore and upsert into local DB.
     * Called on app launch from MainViewModel.
     */
    suspend fun syncFromCloud(marketDao: MarketDao) {
        try {
            val snapshot = sharedBuyersCollection.get().await()
            if (snapshot.isEmpty) {
                // If Firestore is empty, seed it with the community buyers from local DB so everyone gets them
                val localCommunityBuyers = marketDao.getAllBuyersSync().filter { it.is_community }
                for (b in localCommunityBuyers) {
                    pushBuyerToCloud(b)
                }
                Log.d(TAG, "Seeded ${localCommunityBuyers.size} community buyers to Firestore.")
            } else {
                val cloudBuyers = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val phone = doc.getString("phone") ?: return@mapNotNull null
                    val type = doc.getString("type") ?: "Buyer"
                    BuyerEntity(
                        id = 0, // Room auto-generates; we match by phone below
                        name = name,
                        phone = phone,
                        email = doc.getString("email"),
                        location = doc.getString("location"),
                        county = doc.getString("county"),
                        sub_county = doc.getString("sub_county"),
                        ward = doc.getString("ward"),
                        type = type,
                        notes = doc.getString("notes"),
                        is_community = true,
                        is_synced = true
                    )
                }

                // Upsert: for each cloud buyer, insert only if no local buyer with same phone exists
                val existingPhones = marketDao.getAllBuyersSync().map { it.phone }.toSet()
                var added = 0
                for (buyer in cloudBuyers) {
                    if (buyer.phone !in existingPhones) {
                        marketDao.insertBuyer(buyer)
                        added++
                    }
                }
                Log.d(TAG, "Synced from Firestore: ${cloudBuyers.size} cloud buyers, $added newly inserted.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync failed (offline?): ${e.message}")
            // Fail silently — local data still works perfectly
        }
    }

    /**
     * Push a buyer to the shared Firestore directory so all farmers see it.
     * Called when a farmer adds a buyer with "Share with all farmers" enabled.
     */
    suspend fun pushBuyerToCloud(buyer: BuyerEntity) {
        try {
            val docId = buyer.phone.replace("+", "").replace(" ", "") // use phone as stable ID
            val data = mapOf(
                "name" to buyer.name,
                "phone" to buyer.phone,
                "email" to (buyer.email ?: ""),
                "location" to (buyer.location ?: ""),
                "county" to (buyer.county ?: ""),
                "sub_county" to (buyer.sub_county ?: ""),
                "ward" to (buyer.ward ?: ""),
                "type" to buyer.type,
                "notes" to (buyer.notes ?: ""),
                "added_at" to com.google.firebase.Timestamp.now()
            )
            sharedBuyersCollection.document(docId).set(data, SetOptions.merge()).await()
            Log.d(TAG, "Buyer '${buyer.name}' pushed to Firestore shared directory.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to push buyer to Firestore: ${e.message}")
        }
    }
}
