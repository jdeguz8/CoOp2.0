package com.jdeguzman.coop2demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.jdeguzman.coop2demo.ui.theme.CoOp2DemoTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // quick test write
        FirebaseFirestore.getInstance()
            .collection("demo")
            .add(mapOf("msg" to "Hello Firebase", "time" to System.currentTimeMillis()))

        setContent {
            CoOp2DemoTheme {
                MyStoresScreen()
            }
        }
}

@Composable
fun SimpleMapScreen() {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(49.8951, -97.1384), 10f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    )
}
}

@Composable
fun MyStoresScreen(
    viewModel: MyStoresViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var storeName by remember { mutableStateOf("") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(49.8951, -97.1384),
            11f
        )
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapLongClick = { latLng ->
                storeName = ""
                viewModel.onMapLongClick(latLng)
            }
        ) {
            uiState.stores.forEach { store ->
                Marker(
                    state = MarkerState(
                        position = LatLng(store.latitude, store.longitude)
                    ),
                    title = store.name
                )
            }
        }

        if (uiState.pendingLatLng != null) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.cancelAddStore()
                    storeName = ""
                },
                title = { Text("Add Store") },
                text = {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Store name") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveStore(storeName)
                            storeName = ""
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            viewModel.cancelAddStore()
                            storeName = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


data class MyStoresUiState(
    val stores: List<StoreLocation> = emptyList(),
    val pendingLatLng: LatLng? = null
)

class MyStoresViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(MyStoresUiState())
    val uiState: StateFlow<MyStoresUiState> = _uiState

    init {
        observeStores()
    }

    private fun observeStores() {
        firestore.collection("stores")
            .whereEqualTo("userId", "demoUser")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val stores = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(StoreLocation::class.java)?.copy(id = doc.id)
                }

                _uiState.update { it.copy(stores = stores) }
            }
    }

    fun onMapLongClick(latLng: LatLng) {
        _uiState.update { it.copy(pendingLatLng = latLng) }
    }

    fun saveStore(name: String) {
        val latLng = _uiState.value.pendingLatLng ?: return

        val store = StoreLocation(
            name = name.ifBlank { "Unnamed Store" },
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            userId = "demoUser"
        )

        viewModelScope.launch {
            firestore.collection("stores").add(store)
            // Firestore snapshotListener will refresh stores list
            _uiState.update { it.copy(pendingLatLng = null) }
        }
    }

    fun cancelAddStore() {
        _uiState.update { it.copy(pendingLatLng = null) }
    }
}

