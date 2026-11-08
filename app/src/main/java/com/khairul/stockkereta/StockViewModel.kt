package com.khairul.stockkereta

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StockViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _cars = MutableStateFlow<List<Car>>(emptyList())
    val cars: StateFlow<List<Car>> = _cars

    init {
        if (auth.currentUser == null) auth.signInAnonymously()
            .addOnSuccessListener { listenCars() }
            .addOnFailureListener { _cars.value = emptyList() }
        else listenCars()
    }

    private fun listenCars() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("cars")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                _cars.value = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Car::class.java)?.copy(id = doc.id)
                }
            }
    }

    fun addCar(car: Car, onDone: (String?) -> Unit = {}) {
        val uid = auth.currentUser?.uid
        if (uid == null) { onDone("Akaun belum disahkan"); return }
        db.collection("users").document(uid).collection("cars")
            .add(car.copy(id = ""))
            .addOnSuccessListener { onDone(null) }
            .addOnFailureListener { onDone(it.localizedMessage) }
    }

    fun sellCar(id: String, price: Double, onDone: (String?) -> Unit = {}) {
        val uid = auth.currentUser?.uid
        if (uid == null) { onDone("Akaun belum disahkan"); return }
        db.collection("users").document(uid).collection("cars").document(id)
            .update(mapOf("salePrice" to price, "status" to "TERJUAL"))
            .addOnSuccessListener { onDone(null) }
            .addOnFailureListener { onDone(it.localizedMessage) }
    }

    fun deleteCar(id: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("cars").document(id).delete()
    }
}
