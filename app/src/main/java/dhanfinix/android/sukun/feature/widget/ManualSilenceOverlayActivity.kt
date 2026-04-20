package dhanfinix.android.sukun.feature.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dhanfinix.android.sukun.core.designsystem.SukunTheme
import dhanfinix.android.sukun.feature.volume.VolumeEvent
import dhanfinix.android.sukun.feature.volume.VolumeViewModel
import dhanfinix.android.sukun.feature.volume.components.ManualSilenceBottomSheet

class ManualSilenceOverlayActivity : ComponentActivity() {

    private val volumeVm: VolumeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by volumeVm.uiState.collectAsState()
            val appTheme by volumeVm.appTheme.collectAsState()
            val useDynamicColor by volumeVm.useDynamicColor.collectAsState()
            
            SukunTheme(
                appTheme = appTheme,
                dynamicColor = useDynamicColor
            ) {
                // We use a Box as a container to catch dismissals if clicking outside the sheet
                Box(modifier = Modifier.fillMaxSize()) {
                    ManualSilenceBottomSheet(
                        onDismiss = { finish() },
                        onStart = { duration ->
                            volumeVm.onEvent(VolumeEvent.StartManualSilence(duration))
                        }
                    )
                }
            }

            // Finish the activity once silence successfully starts
            LaunchedEffect(uiState.isSukunActive) {
                if (uiState.isSukunActive) {
                    finish()
                }
            }
        }
    }
}
