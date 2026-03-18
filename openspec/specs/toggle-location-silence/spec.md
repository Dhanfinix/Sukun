# Capability: Toggle Location Silence

## Overview
Provides a tabbed interface for managing different types of silent zones, ensuring a clear distinction between user-defined and auto-detected locations.

## Requirements

### Requirement: Tabbed Layout for Silent Zones
The Silent Zones management interface SHALL use a tabbed layout to separate manually added zones from auto-detected mosques.

#### Scenario: Switching tabs
- **WHEN** the user selects the "Mosques" tab
- **THEN** the system SHALL display the list of nearby auto-detected mosques and the "Auto-Mosque Silence" master toggle.

### Requirement: Specialized Mosque Markers
The map view SHALL use a specialized mosque icon for auto-detected mosque markers.

#### Scenario: Rendering mosque markers
- **WHEN** a silent zone with `source == AUTO_MOSQUE` is rendered on the map
- **THEN** the system SHALL display a mosque icon instead of the default circular marker.
