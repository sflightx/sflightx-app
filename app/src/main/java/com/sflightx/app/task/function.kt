package com.sflightx.app.task

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sflightx.app.UserData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

class function {
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun getUserByUid(uid: String): UserData? = suspendCancellableCoroutine { cont ->
    val userRef = FirebaseDatabase.getInstance().getReference("userdata").child(uid)

    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val userData = snapshot.getValue(UserData::class.java)
            cont.resume(userData) {}
        }

        override fun onCancelled(error: DatabaseError) {
            cont.resume(null) {}
        }
    }

    userRef.addListenerForSingleValueEvent(listener)

    cont.invokeOnCancellation {
        userRef.removeEventListener(listener)
    }
}