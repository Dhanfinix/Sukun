## 1. Data Model & Storage

- [x] 1.1 Update `SilentZone` entity to include `source` (Enum: MANUAL, AUTO_MOSQUE) and `externalId` (String?).
- [x] 1.2 Implement Room database migration to add new columns.
- [x] 1.3 Add `isAutoMosqueSilenceEnabled` to `UserPreferences` and `MainViewModel`.

## 2. API & Network

- [x] 2.1 Define `OverpassService` for querying OSM data.
- [x] 2.2 Add `OverpassService` to `ApiClient`.
- [x] 2.3 Implement `MosqueRepository` to fetch and parse mosque data from Overpass API.

## 3. Core Logic & Background

- [x] 3.1 Implement `SilentZonesViewModel.fetchNearbyMosques()` using the repository.
- [x] 3.2 Update `GeofenceReceiver` to check `isAutoMosqueSilenceEnabled` when processing `AUTO_MOSQUE` zones.
- [x] 3.3 Add footprint-based radius calculation logic.

## 4. UI Implementation (Map Screen)

- [x] 4.1 Update `SilentZonesBottomOverlay` to include a `TabRow` (Custom vs. Mosques).
- [x] 4.2 Add "Auto-Mosque Silence" master toggle at the top of the Mosques tab list.
- [x] 4.3 Update `OsmMapView` to render a mosque icon for `AUTO_MOSQUE` markers.
- [x] 4.4 Ensure map icons change color based on `isEnabled` and `isActive` state.

## 5. Localization & Resources

- [x] 5.1 Add specialized mosque icon drawable.
- [x] 5.2 Add new string resources for "Auto-Mosque Silence", "Custom", "Mosques", etc.
- [x] 5.3 Localize new strings in Indonesian and Arabic.
