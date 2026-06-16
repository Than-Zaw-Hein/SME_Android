package com.tzh.sme.ui.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.tzh.sme.ui.theme.SMETheme
import java.util.Locale

class LocationPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val initialLat = intent.getDoubleExtra("latitude", 16.8409) // Default Yangon
        val initialLng = intent.getDoubleExtra("longitude", 96.1735)
        
        setContent {
            SMETheme {
                LocationPickerContent(
                    initialLocation = LatLng(initialLat, initialLng),
                    onLocationSelected = { location ->
                        val resultIntent = android.content.Intent().apply {
                            putExtra("latitude", location.latitude)
                            putExtra("longitude", location.longitude)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
fun LocationPickerContent(
    initialLocation: LatLng,
    onLocationSelected: (LatLng) -> Unit,
    onCancel: () -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 15f)
    }
    
    // We'll use the center of the map as the picked location
    val pickedLocation = cameraPositionState.position.target

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        )

        // Center Pin
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.Center)
                .offset(y = (-20).dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Bottom Controls
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Selected Location", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "${String.format(Locale.US, "%.5f", pickedLocation.latitude)}, ${String.format(Locale.US, "%.5f", pickedLocation.longitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Row {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onLocationSelected(pickedLocation) }) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Confirm")
                    }
                }
            }
        }
    }
}
