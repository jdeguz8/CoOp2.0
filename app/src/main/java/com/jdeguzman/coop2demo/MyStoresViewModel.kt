package com.jdeguzman.coop2demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NearMeUiState(
    val notes: List<LocationNote> = emptyList(),
    val pendingLatLng: LatLng? = null
)


class NearMeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(NearMeUiState())
    val uiState: StateFlow<NearMeUiState> = _uiState

    init {
        observeNotes()
    }

    private fun observeNotes() {
        firestore.collection("notes") // 👈 was "stores"
            .whereEqualTo("userId", "demoUser")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val notes = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(LocationNote::class.java)?.copy(id = doc.id)
                }

                _uiState.update { it.copy(notes = notes) }
            }
    }

    fun onMapLongClick(latLng: LatLng) {
        _uiState.update { it.copy(pendingLatLng = latLng) }
    }

    fun saveNote(title: String, description: String, radiusMeters: Float) {
        val latLng = _uiState.value.pendingLatLng ?: return

        val note = LocationNote(
            title = title.ifBlank { "Untitled reminder" },
            description = description,
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            radiusMeters = radiusMeters,
            userId = "demoUser"
        )

        viewModelScope.launch {
            firestore.collection("notes").add(note)
            _uiState.update { it.copy(pendingLatLng = null) }
        }
    }

    fun cancelAddNote() {
        _uiState.update { it.copy(pendingLatLng = null) }
    }
}

