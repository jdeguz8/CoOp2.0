package com.jdeguzman.coop2demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.jdeguzman.coop2demo.ui.theme.CoOp2DemoTheme
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.maps.android.compose.MapProperties
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api


class MainActivity : ComponentActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request location permission if not granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        setContent {
            CoOp2DemoTheme {
                NearMeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearMeScreen(
    viewModel: NearMeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                }
            }
        }
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(100f) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation ?: LatLng(49.8951, -97.1384),
            12f
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NearMe") }
            )
        }
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission
                ),
                onMapLongClick = { latLng ->
                    title = ""
                    description = ""
                    radius = 100f
                    viewModel.onMapLongClick(latLng)
                }
            ) {
                uiState.notes.forEach { note ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(note.latitude, note.longitude)
                        ),
                        title = note.title,
                        snippet = note.description
                    )
                }
            }

            // Nicer overlay card
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "NearMe reminders",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (uiState.notes.isEmpty()) {
                        Text(
                            text = "Long-press on the map to add a reminder.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        uiState.notes.forEach { note ->
                            val distanceMeters = userLocation?.let { userLatLng ->
                                distanceMeters(userLatLng, LatLng(note.latitude, note.longitude))
                            }

                            val distanceLabel = distanceMeters?.let {
                                "${it.toInt()} m away"
                            } ?: "Distance unknown"

                            Text(
                                text = "• ${note.title} – $distanceLabel",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (uiState.pendingLatLng != null) {
                AlertDialog(
                    onDismissRequest = {
                        viewModel.cancelAddNote()
                        title = ""
                        description = ""
                    },
                    title = { Text("Add NearMe reminder") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Title") }
                            )
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description (optional)") }
                            )
                            Text(text = "Radius (static for now): ${radius.toInt()} m")
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.saveNote(
                                    title = title,
                                    description = description,
                                    radiusMeters = radius
                                )
                                title = ""
                                description = ""
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                viewModel.cancelAddNote()
                                title = ""
                                description = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

fun distanceMeters(from: LatLng, to: LatLng): Float {
    val result = FloatArray(1)
    Location.distanceBetween(
        from.latitude, from.longitude,
        to.latitude, to.longitude,
        result
    )
    return result[0] // meters
}
