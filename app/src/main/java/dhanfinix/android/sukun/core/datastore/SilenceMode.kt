package dhanfinix.android.sukun.core.datastore

/**
 * Defines how Sukun should silence the phone during prayer times.
 */
enum class SilenceMode {
    /**
     * Sets the phone's Ringer Mode to Silent.
     * Notifications will still appear visually, but the device will make no sound.
     */
    DND,

    /**
     * Uses Android's Do Not Disturb (DND)/Interruption Filter feature.
     * Blocks visual notifications and sounds (depending on system DND settings).
     */
    SILENT,

    /**
     * Sets the phone's Ringer Mode to Vibrate.
     * Notifications will still appear visually and the device will vibrate, but make no sound.
     */
    VIBRATE
}
