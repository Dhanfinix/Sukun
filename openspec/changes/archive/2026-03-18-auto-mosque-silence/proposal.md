## Why

Currently, users must manually identify and add "Silent Zones" for mosques. This can be repetitive and users might forget to add a new mosque they visit. Automatically identifying and suggesting/adding mosques as silent zones significantly enhances the "set and forget" nature of the app, providing a more seamless experience for users who frequently visit different mosques.

## What Changes

- Automatically fetch nearby mosques (Masjid, Musholla, prayer rooms) using OpenStreetMap data (Overpass API).
- Introduce a new category of "Auto-Detected Mosques" in the Silent Zones screen.
- Add a master toggle for "Auto-Mosque Silence" within the Location Silence settings.
- Implement a Tab layout in the Silent Zones list to separate "Custom Zones" (user-added) and "Detected Mosques".
- Render detected mosques on the map with a specialized mosque icon.
- **BREAKING**: Update `SilentZone` entity to distinguish between manual and auto-detected zones.

## Capabilities

### New Capabilities
- `auto-mosque-detection`: Logic to fetch nearby mosques from OSM data based on user location.
- `auto-mosque-silence`: Requirements for automatic silencing behavior when entering detected mosque areas.

### Modified Capabilities
- `toggle-location-silence`: Update to include "Auto-Mosque Silence" as a sub-toggle and UI refinements for tabs.

## Impact

- `SilentZone` entity and DAO: Added fields to identify source (manual vs. auto).
- `UserPreferences`: New setting for auto-mosque silence.
- `SilentZonesViewModel`: New logic for fetching and managing auto-detected zones.
- `OsmMapView`: New icon rendering logic for mosque markers.
- `SilentZonesBottomOverlay`: UI update to support Tab layout and master toggle for mosques.
