## ADDED Requirements

### Requirement: Global Toggle for Auto-Mosque Silence
The application SHALL provide a master switch specifically for auto-detected mosques to enable or disable all mosque-based silence triggers at once.

#### Scenario: Disable Auto-Mosque Silence
- **WHEN** the "Auto-Mosque Silence" master toggle is OFF
- **THEN** entering or dwelling in an `AUTO_MOSQUE` zone SHALL NOT trigger any silence actions or notifications, even if individual zones are enabled.

### Requirement: Individual Mosque Toggling
The system SHALL allow users to enable or disable individual auto-detected mosques.

#### Scenario: Disable specific mosque
- **WHEN** a user turns OFF the toggle for a specific detected mosque
- **THEN** that mosque SHALL NOT trigger silence actions, regardless of the global "Auto-Mosque Silence" state.
