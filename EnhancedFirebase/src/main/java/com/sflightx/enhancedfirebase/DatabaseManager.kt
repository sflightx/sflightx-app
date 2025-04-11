package com.sflightx.enhancedfirebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class DatabaseManager {
    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference
    }

    // Add data to Realtime DatabaseManager
    fun addData(
        path: String,
        data: Any,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        database.child(path).setValue(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Retrieve data from Realtime DatabaseManager
    fun getData(
        path: String,
        onSuccess: (Any?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        database.child(path).get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.value)
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Remove data from Realtime DatabaseManager
    fun removeData(
        path: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        database.child(path).removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}