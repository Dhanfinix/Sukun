package dhanfinix.android.sukun.core.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.feature.mosque.data.MosqueRepository
import dhanfinix.android.sukun.feature.prayer.data.PrayerRepository
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerInfo
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName
import dhanfinix.android.sukun.worker.SilenceScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.time.LocalDate

class GeofenceForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var userPrefs: UserPreferences
    private lateinit var mosqueRepo: MosqueRepository
    private lateinit var prayerRepo: PrayerRepository
    private lateinit var scheduler: SilenceScheduler

    override fun onCreate() {
        super.onCreate()
        userPrefs = UserPreferences(this)
        mosqueRepo = MosqueRepository(this)
        prayerRepo = PrayerRepository(this)
        scheduler = SilenceScheduler(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.fg_service_notification_title))
            .setContentText(getString(R.string.fg_service_notification_desc))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
            
        startForeground(NOTIFICATION_ID, notification)
        
        requestLocationUpdates()
        
        return START_STICKY
    }

    private fun requestLocationUpdates() {
        // 5 minutes interval, high accuracy for instant triggers
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 
            5 * 60 * 1000L
        ).setMinUpdateDistanceMeters(100f).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e("GeofenceFGService", "Lost location permission", unlikely)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->
                serviceScope.launch {
                    processNewLocation(location)
                }
            }
        }
    }

    private suspend fun processNewLocation(location: Location) {
        // Check if we traveled significantly before doing any work
        val savedLat = userPrefs.latitude.first()
        val savedLng = userPrefs.longitude.first()
        
        val results = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, savedLat, savedLng, results)
        val distanceMeters = results[0]

        // Only fetch mosques if moved > 500m from saved position (reduces API calls)
        if (distanceMeters > 500f) {
            val isAutoMosque = userPrefs.isAutoMosqueSilenceEnabled.first()
            if (isAutoMosque) {
                try {
                    mosqueRepo.fetchAndSaveMosques(location.latitude, location.longitude)
                } catch (e: Exception) {
                    Log.e("GeofenceFGService", "Error fetching mosques", e)
                }
            }
        }

        // If moved > 10km, update prayer times for new region
        if (distanceMeters > 10_000f) {
            Log.d("GeofenceFGService", "Moved >10km. Updating prayer times...")
            userPrefs.setLocation(location.latitude, location.longitude)
            recalculatePrayerTimes()
        }
    }

    private suspend fun recalculatePrayerTimes() {
        val lat = userPrefs.latitude.first()
        val lng = userPrefs.longitude.first()
        val method = userPrefs.calculationMethod.first()
        val durations = userPrefs.prayerDurations.first()
        val offsets = userPrefs.prayerOffsets.first()
        val enabledMap = userPrefs.isPrayerEnabled.first()

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
                
        val resultToday = prayerRepo.getPrayerTimes(today, lat, lng, method, offsets)
        val resultTomorrow = prayerRepo.getPrayerTimes(tomorrow, lat, lng, method, offsets)

        if (resultToday.isSuccess && resultTomorrow.isSuccess) {
            val timesMapToday = resultToday.getOrThrow()
            val timesMapTomorrow = resultTomorrow.getOrThrow()
                    
            val prayersToday = PrayerName.entries.map { name ->
                PrayerInfo(name = name, time = timesMapToday[name] ?: "--:--", isEnabled = enabledMap[name] ?: true)
            }
                    
            val prayersTomorrow = PrayerName.entries.map { name ->
                PrayerInfo(name = name, time = timesMapTomorrow[name] ?: "--:--", isEnabled = enabledMap[name] ?: true)
            }
                    
            scheduler.scheduleAll(prayersToday, prayersTomorrow, durations, offsets)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.fg_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.fg_service_channel_desc)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "GeofenceForegroundChannel"
        private const val NOTIFICATION_ID = 888
    }
}
