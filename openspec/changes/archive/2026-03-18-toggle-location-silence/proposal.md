## Why

Users currently have no way to globally disable "Silent Zones" (location silence) without deleting all their zones or disabling auto-silence for each zone individualy. This is inconvenient if they temporarily want to disable the feature (e.g., when they are at a mosque but don't want the phone to be silenced for a specific reason). Adding a master toggle provides a quick and intuitive way to control this feature.

## What Changes

- Add a global "Location Silence" master toggle in the Settings screen.
- Add a "Location Silence" toggle in the Home screen's Location Silence section for quick access.
- Update `GeofenceReceiver` to honor this global setting before triggering any silence actions.
- Update `UserPreferences` to store this new setting.
- Update relevant ViewModels to expose and handle this setting.

## Capabilities

### New Capabilities
- `toggle-location-silence`: Provides a global switch to enable or disable all location-based silence triggers.

### Modified Capabilities
- `silent-zones`: Existing geofencing behavior will now be gated by the global `toggle-location-silence` setting.

## Impact

- **Core**: `UserPreferences.kt` will have a new boolean key.
- **Worker**: `GeofenceReceiver.kt` will check the new setting.
- **UI**: `HomeScreen.kt`, `LocationSilenceSection.kt`, and `SettingsScreen.kt` will be updated with the new toggle.
- **ViewModels**: `MainViewModel.kt`, `VolumeViewModel.kt`, and `SilentZonesViewModel.kt` will handle the state.
