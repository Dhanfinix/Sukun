## Context

Currently, `SilentZone` is a simple entity representing user-defined circular areas. The app uses `GeofenceManager` to register these zones with the OS. We want to add auto-detected mosques from OpenStreetMap (OSM) as a second layer of silent zones.

## Goals / Non-Goals

**Goals:**
- Automatically fetch and store mosque data near the user.
- Distinguish between user-created zones and auto-detected mosques.
- Allow global enabling/disabling of auto-mosque silence.
- Show mosques with a specialized icon on the map.
- Implement a Tab layout for zone management.
- **Auto-Radius**: If the mosque data includes a footprint (polygon/way), use it to approximate a radius or define a polygon geofence.

**Non-Goals:**
- Allowing users to edit auto-detected mosque locations (they can only toggle them or use the global switch).
- Real-time tracking of every mosque in the world (only fetch nearby ones based on current location).

## Decisions

### 1. Data Model Update
Update `SilentZone` to include a `source` field.
- `source`: Enum (MANUAL, AUTO_MOSQUE).
- `externalId`: String? (to store OSM ID for auto-detected zones, preventing duplicates).

### 2. Fetching Mosque Data
Use the **Overpass API** to query for `amenity=mosque` or `religion=muslim` within a certain radius (e.g., 5km) of the user's current location.
- **Frequency**: Fetch when the user opens the Map screen and has moved significantly since the last fetch.
- **Auto-Radius**: Overpass API can return `center` coordinates for ways/relations. If footprint data is available, we will calculate the bounding box or use the maximum distance from center to a node as the radius (clamped between 50m and 300m). If only a point is provided, default to 100m.

### 3. Geofencing Strategy
Both manual and enabled auto-mosques will be registered as geofences.
- Auto-mosques will be synced to the local database to allow offline geofencing once detected.

### 4. UI/UX: Tab Layout
The `SilentZonesBottomOverlay` will be updated to use a `TabRow` with two tabs:
- **Custom**: Existing functionality for manual zones.
- **Mosques**: List of nearby detected mosques with a master switch at the top.

### 5. Map Rendering
Update `OsmMapView` to use a `mosque` icon for markers where `source == AUTO_MOSQUE`.

## Risks / Trade-offs

- **[Risk] Overpass API Rate Limits** → **Mitigation**: Cache results locally and only fetch when the user moves > 1km.
- **[Risk] Inaccurate OSM Data** → **Mitigation**: Allow users to "Disable" specific auto-detected mosques if they are incorrect.
- **[Risk] Geofence Limit (100 per app)** → **Mitigation**: Only register the closest 50 manual zones and closest 50 enabled auto-mosques.

## Migration Plan
- Add `source` and `externalId` columns to `silent_zones` table via Room migration.
- Default existing rows to `source = MANUAL`.

## Open Questions
- Should we automatically enable all detected mosques, or require the user to "Opt-in" to each one? 
  - *Decision*: If the global "Auto-Mosque Silence" is ON, we should default new detections to `isEnabled = true` but allow individual toggling.
