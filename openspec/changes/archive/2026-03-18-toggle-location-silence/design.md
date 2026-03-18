## Context

The "Silent Zones" feature automatically silences the phone when a user enters a predefined geographic area (geofence). Currently, there is no global master switch to disable this feature.

## Goals / Non-Goals

**Goals:**
- Provide a global master switch to enable/disable all Silent Zones.
- Ensure the switch is easily accessible from both the Home and Settings screens.
- Ensure the `GeofenceReceiver` immediately honors the switch state.
- Maintain consistency with existing design patterns for toggles in the app.

**Non-Goals:**
- This change does not aim to modify the geofence registration/unregistration logic itself, only to gate the *triggering* of silence actions.
- This change does not affect the manual silence feature or prayer-based silence.

## Decisions

### 1. Data Storage
- **Decision**: Add `KEY_SILENT_ZONES_ENABLED` (boolean) to `UserPreferences.kt`.
- **Rationale**: `UserPreferences` is the established single source of truth for all user settings in the app.

### 2. Logic Gating
- **Decision**: Add a check for `isSilentZonesEnabled` at the start of `GeofenceReceiver.onReceive`.
- **Rationale**: This is the most efficient point to gate the feature, preventing any database lookups or broadcast emissions if the feature is disabled.

### 3. UI Integration
- **Decision**: 
    - Add a `Switch` to the `LocationSilenceSection` on the Home screen.
    - Add a `SettingsItem` with a `Switch` to the `SettingsScreen` under a new or existing section.
- **Rationale**: Dual placement ensures both quick access and standard settings management.

### 4. ViewModel State
- **Decision**: Expose `isSilentZonesEnabled` in `MainViewModel` (for Settings) and `VolumeViewModel` (for Home).
- **Rationale**: Following the existing architecture where ViewModels bridge DataStore and UI.

## Risks / Trade-offs

- **Risk**: User might forget they disabled Silent Zones and expect it to work.
- **Mitigation**: Update the UI state in `LocationSilenceSection` to clearly show "Location Silence is Disabled" when the toggle is off.
