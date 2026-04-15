## ADDED Requirements

### Requirement: Global Toggle for Location Silence
The application shall provide a global master switch to enable or disable all location-based silence functionality.

#### Scenario: Disable Location Silence
- **WHEN** the user turns OFF the "Location Silence" master switch
- **THEN** all existing "Silent Zones" shall remain in the database but shall NOT trigger any volume changes or notifications upon entry or dwell.

#### Scenario: Enable Location Silence
- **WHEN** the user turns ON the "Location Silence" master switch
- **THEN** entering or dwelling in an enabled "Silent Zone" shall trigger the configured silence actions (Auto-Silent or notification).

#### Scenario: Status Visibility on Home Screen
- **WHEN** "Location Silence" is disabled
- **THEN** the Home screen's Location Silence section shall display a message indicating that the feature is currently disabled and provide a way to re-enable it.
