## 1. Core Data Layer

- [x] 1.1 Add `KEY_LOCATION_SILENCE_ENABLED` to `UserPreferences.kt`.
- [x] 1.2 Add `isLocationSilenceEnabled` Flow to `UserPreferences.kt`.
- [x] 1.3 Add `setLocationSilenceEnabled(enabled: Boolean)` suspend function to `UserPreferences.kt`.

## 2. Background Logic Gating

- [x] 2.1 Update `GeofenceReceiver.kt` to check `userPrefs.isLocationSilenceEnabled.first()` before processing transitions.

## 3. ViewModel Updates

- [x] 3.1 Update `MainViewModel.kt` to expose `isLocationSilenceEnabled` StateFlow and add `setLocationSilenceEnabled` function.
- [x] 3.2 Update `VolumeViewModel.kt` to observe `isLocationSilenceEnabled` and update `VolumeUiState`.
- [x] 3.3 Update `VolumeUiState.kt` to include `isLocationSilenceEnabled`.
- [x] 3.4 Update `SilentZonesViewModel.kt` as needed for state consistency.

## 4. UI Implementation (Home Screen)

- [x] 4.1 Update `LocationSilenceSection.kt` to include a `Switch` for enabling/disabling the feature.
- [x] 4.2 Update the text display in `LocationSilenceSection` to show a "Disabled" state when the toggle is off.

## 5. UI Implementation (Settings Screen)

- [x] 5.1 Add a new `SettingsItem` in `SettingsScreen.kt` for "Location Silence" with a `Switch`.
- [x] 5.2 Add a descriptive subtitle for the new setting.

## 6. Resources & Localization

- [x] 6.1 Add new string resources for the toggle labels and descriptions in `strings.xml`.

## 7. Refinement & Bug Fixes

- [x] 7.1 Revamp the `Switch` in `LocationSilenceSection.kt` for better UI integration.
- [x] 7.2 Add the "Location Silence" master toggle to the `SilentZonesScreen.kt` (Map Screen).
- [x] 7.3 Implement a mechanism to trigger silence if the user is already inside a geofence when the master toggle is turned ON.
- [x] 7.4 Fix the silence re-trigger loop when manually stopping the countdown while inside a zone.

