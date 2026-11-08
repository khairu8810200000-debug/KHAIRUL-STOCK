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

    private var signInInProgress = false
    private val pendingAuthCallbacks = mutableListOf<(String?) -> Unit>()

    init {
        ensureAuthenticated()
    }

    private fun ensureAuthenticated(onReady: (String?) -> Unit = {}) {
        val existingUser = auth.currentUser
        if (existingUser != null) {
            listenCars()
            onReady(null)
            return
        }

        pendingAuthCallbacks.add(onReady)
        if (signInInProgress) return

        signInInProgress = true
        auth.signInAnonymously()
            .addOnSuccessListener {
                signInInProgress = false
                listenCars()
                val callbacks = pendingAuthCallbacks.toList()
                pendingAuthCallbacks.clear()
                callbacks.forEach { it(null) }
            }
            .addOnFailureListener { error ->
                signInInProgress = false
                val callbacks = pendingAuthCallbacks.toList()
                pendingAuthCallbacks.clear()
                val message = error.localizedMessage ?: "Gagal mengesahkan akaun Firebase"
                callbacks.forEach { it(message) }
            }
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
        ensureAuthenticated { authError ->
            if (authError != null) {
                onDone(authError)
                return@ensureAuthenticated
            }
            val uid = auth.currentUser?.uid
            if (uid == null) {
                onDone("Firebase belum mengesahkan akaun. Cuba sekali lagi.")
                return@ensureAuthenticated
            }
            db.collection("users").document(uid).collection("cars")
                .add(car.copy(id = ""))
                .addOnSuccessListener { onDone(null) }
                .addOnFailureListener { onDone(it.localizedMessage ?: "Gagal menyimpan rekod") }
        }
    }

    fun sellCar(id: String, price: Double, onDone: (String?) -> Unit = {}) {
        ensureAuthenticated { authError ->
            if (authError != null) {
                onDone(authError)
                return@ensureAuthenticated
            }
            val uid = auth.currentUser?.uid
            if (uid == null) {
                onDone("Firebase belum mengesahkan akaun. Cuba sekali lagi.")
                return@ensureAuthenticated
            }
            db.collection("users").document(uid).collection("cars").document(id)
                .update(mapOf("salePrice" to price, "status" to "TERJUAL"))
                .addOnSuccessListener { onDone(null) }
                .addOnFailureListener { onDone(it.localizedMessage ?: "Gagal mengemas kini jualan") }
        }
    }

    fun deleteCar(id: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("cars").document(id).delete()
    }
}
