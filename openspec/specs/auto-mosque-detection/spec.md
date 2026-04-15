# Capability: Auto-Mosque Detection

## Overview
The system automatically identifies nearby mosques using OpenStreetMap data to provide location-based silencing features.

## Requirements

### Requirement: Nearby Mosque Detection
The system SHALL automatically fetch and store information about mosques within a 5km radius of the user's current location using OpenStreetMap data.

#### Scenario: Successful detection
- **WHEN** the user opens the "Silent Zones" map and the system has a valid location fix
- **THEN** the system SHALL query the Overpass API for nearby mosques and save them to the local database as `AUTO_MOSQUE` zones if they don't already exist.

### Requirement: Automatic Radius Calculation
The system SHALL attempt to calculate an appropriate silence radius based on the mosque's footprint if available in the OSM data.

#### Scenario: Footprint available
- **WHEN** a mosque is detected as a 'way' or 'relation' with a footprint
- **THEN** the system SHALL set the zone radius to the maximum distance from the center to any point in the footprint, clamped between 50m and 300m.

#### Scenario: Footprint unavailable
- **WHEN** a mosque is detected as a 'node' (point) without footprint data
- **THEN** the system SHALL default the zone radius to 100m.
