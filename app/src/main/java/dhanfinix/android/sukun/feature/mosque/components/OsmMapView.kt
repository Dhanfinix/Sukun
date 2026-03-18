package dhanfinix.android.sukun.feature.mosque.components

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import dhanfinix.android.sukun.core.database.entity.SilentZone
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.utils.withIsolate
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import kotlin.math.abs

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    zones: List<SilentZone> = emptyList(),
    userLat: Double? = null,
    userLng: Double? = null,
    isUserLocationFresh: Boolean = true,
    searchLat: Double? = null,
    searchLng: Double? = null,
    searchLabel: String? = null,
    centerLat: Double? = null,
    centerLng: Double? = null,
    zoom: Double = 18.0,
    focusedZoneId: Long? = null,
    onMapClick: () -> Unit = {},
    onMapLongClick: (Double, Double) -> Unit = { _, _ -> },
    onMarkerClick: (SilentZone) -> Unit = {},
    onCenterChanged: (Double, Double) -> Unit = { _, _ -> },
    onZoomChanged: (Double) -> Unit = {},
    onAddFromSearch: (String, Double, Double) -> Unit = { _, _, _ -> },
    cameraRequest: CameraRequest? = null
) {
    val context = LocalContext.current
    
    // OSM configuration - Load BEFORE MapView is created if possible, or right after
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm_pref", 0))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(true)
            
            // No hardcoded default here anymore; center is set by factory or cameraRequest
            controller.setZoom(zoom)
        }
    }
    
    val zoneInfoWindow = remember(mapView) { SilentZoneInfoWindow(mapView) }
    val searchInfoWindow = remember(mapView) { SearchResultInfoWindow(mapView, onAddFromSearch) }

    // Handle Map Click and Long Click
    val clickOverlay = remember {
        object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                onMapClick()
                return true
            }

            override fun onLongPress(e: MotionEvent, mapView: MapView): Boolean {
                val projection = mapView.projection
                val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                onMapLongClick(geoPoint.latitude, geoPoint.longitude)
                return true
            }
        }
    }

    DisposableEffect(mapView) {
        onDispose {
            mapView.onDetach()
        }
    }

    // Apply zoom whenever the parameter changes (forced from parent)
    LaunchedEffect(zoom) {
        if (abs(mapView.zoomLevelDouble - zoom) > 0.1) {
            mapView.controller.setZoom(zoom)
        }
    }
    

    // Handle Markers & Polygons in a more efficient way
    LaunchedEffect(zones, userLat, userLng, searchLat, searchLng, searchLabel) {
        // Only remove markers and polygons, preserving the clickOverlay
        mapView.overlays.removeAll { it is Marker || it is Polygon }
        
        // Polygons
        zones.forEach { zone ->
            val circle = Polygon(mapView).apply {
                id = "poly_${zone.id}"
                points = Polygon.pointsAsCircle(GeoPoint(zone.latitude, zone.longitude), zone.radius.toDouble())
                val colorHex = if (zone.isEnabled) "#4CAF50" else "#9E9E9E"
                val fillColorHex = if (zone.isEnabled) "#334CAF50" else "#339E9E9E"
                fillPaint.color = fillColorHex.toColorInt()
                outlinePaint.color = colorHex.toColorInt()
                outlinePaint.strokeWidth = 3f * context.resources.displayMetrics.density
            }
            circle.setOnClickListener { _, _, _ ->
                onMarkerClick(zone)
                true
            }
            mapView.overlays.add(circle)
        }

        // Zone Markers
        zones.forEach { zone ->
            val marker = Marker(mapView).apply {
                id = zone.id.toString()
                position = GeoPoint(zone.latitude, zone.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = zone.name
                relatedObject = zone
                infoWindow = zoneInfoWindow
                val density = context.resources.displayMetrics.density
                val size = (14 * density).toInt()
                val strokeWidth = (2 * density).toInt()
                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor((if (zone.isEnabled) "#4CAF50" else "#9E9E9E").toColorInt())
                    setStroke(strokeWidth, android.graphics.Color.WHITE)
                    setSize(size, size)
                }
                icon = shape
                setOnMarkerClickListener { m, _ ->
                    onMarkerClick(zone)
                    m.showInfoWindow()
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        // Search Marker
        if (searchLat != null && searchLng != null) {
            val searchMarker = Marker(mapView).apply {
                id = "search_location"
                position = GeoPoint(searchLat, searchLng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = searchLabel ?: context.getString(R.string.label_search_result)
                icon = ContextCompat.getDrawable(context, R.drawable.ic_search_pin)
                infoWindow = searchInfoWindow
                setOnMarkerClickListener { m, _ ->
                    m.showInfoWindow()
                    true
                }
            }
            mapView.overlays.add(searchMarker)
        }

        // User Location Marker (last so it's on top)
        if (userLat != null && userLng != null) {
            val userMarker = Marker(mapView).apply {
                id = "user_location"
                position = GeoPoint(userLat, userLng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = context.getString(R.string.label_my_location)
                val size = (16 * context.resources.displayMetrics.density).toInt()
                val strokeWidth = (2 * context.resources.displayMetrics.density).toInt()
                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    val colorHex = if (isUserLocationFresh) "#2196F3" else "#9E9E9E"
                    setColor(colorHex.toColorInt())
                    setStroke(strokeWidth, android.graphics.Color.WHITE)
                    setSize(size, size)
                }
                icon = shape
                setInfoWindow(null)
            }
            mapView.overlays.add(userMarker)
        }
        
        mapView.invalidate()
    }

    // Explicit Centering Logic (Only triggers when a new CameraRequest is received)
    LaunchedEffect(cameraRequest) {
        cameraRequest?.let { request ->
            val target = GeoPoint(request.lat, request.lng)
            mapView.controller.animateTo(target)
            mapView.controller.zoomTo(request.zoom)
        }
    }

    // Focused Marker Logic
    LaunchedEffect(focusedZoneId) {
        if (focusedZoneId == null) {
            InfoWindow.closeAllInfoWindowsOn(mapView)
        } else {
            val focusedMarker = mapView.overlays
                .filterIsInstance<Marker>()
                .firstOrNull { it.id == focusedZoneId.toString() || (focusedZoneId == -1L && it.id == "search_location") }
            if (focusedMarker != null) {
                InfoWindow.closeAllInfoWindowsOn(mapView)
                focusedMarker.showInfoWindow()
            }
        }
    }
    
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            mapView.apply {
                if (!overlays.contains(clickOverlay)) {
                    overlays.add(0, clickOverlay)
                }
                
                // Final check on centering during factory
                val lat = centerLat ?: userLat ?: -6.2088
                val lng = centerLng ?: userLng ?: 106.8456
                controller.setCenter(GeoPoint(lat, lng))
                controller.setZoom(zoom)
            }
        },
        update = { /* Updates handled in effects above for better performance */ }
    )

    // Listener for map changes
    DisposableEffect(mapView) {
        val listener = object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                val center = mapView.mapCenter as GeoPoint
                onCenterChanged(center.latitude, center.longitude)
                return true
            }
            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean { 
                onZoomChanged(mapView.zoomLevelDouble)
                return true 
            }
        }
        mapView.addMapListener(listener)
        onDispose {
            mapView.removeMapListener(listener)
        }
    }
}

@Parcelize
data class CameraRequest(
    val lat: Double,
    val lng: Double,
    val zoom: Double = 18.0,
    val id: Long = System.currentTimeMillis()
) : Parcelable

private class SilentZoneInfoWindow(mapView: MapView) :
    MarkerInfoWindow(R.layout.view_silent_zone_popup, mapView) {

    init {
        mView.setBackgroundColor(Color.TRANSPARENT)
        mView.setPadding(0, 0, 0, 0)
    }

    override fun onOpen(item: Any?) {
        val marker = item as? Marker ?: return
        val zone = marker.relatedObject as? SilentZone ?: return
        val view = mView

        val name = view.findViewById<android.widget.TextView>(R.id.zone_name)
        val statusChip = view.findViewById<android.widget.TextView>(R.id.zone_status_chip)
        val modeChip = view.findViewById<android.widget.TextView>(R.id.zone_mode_chip)
        val radius = view.findViewById<android.widget.TextView>(R.id.zone_radius)
        val duration = view.findViewById<android.widget.TextView>(R.id.zone_duration)

        name.text = zone.name
        radius.text = java.lang.String.format(
            java.util.Locale.US,
            view.resources.getString(R.string.radius_value_format),
            zone.radius.toInt()
        )

        val durationMinutes = zone.silenceDuration ?: 30
        duration.text = if (zone.isAutoSilent) {
            if (durationMinutes >= 60) {
                java.lang.String.format(
                    java.util.Locale.US,
                    view.resources.getString(R.string.ends_in_hours),
                    durationMinutes / 60
                )
            } else {
                java.lang.String.format(
                    java.util.Locale.US,
                    view.resources.getString(R.string.ends_in_mins),
                    durationMinutes
                )
            }
        } else {
            view.resources.getString(R.string.status_notification_only)
        }

        statusChip.text = if (zone.isEnabled) {
            view.resources.getString(R.string.status_enabled)
        } else {
            view.resources.getString(R.string.status_disabled)
        }

        modeChip.text = if (zone.isAutoSilent) {
            view.resources.getString(R.string.auto_mute)
        } else {
            view.resources.getString(R.string.notify_only)
        }

        statusChip.background = view.context.getDrawable(
            if (zone.isEnabled) R.drawable.bg_zone_chip_enabled else R.drawable.bg_zone_chip_disabled
        )
        statusChip.setTextColor(
            ContextCompat.getColor(
                view.context,
                if (zone.isEnabled) R.color.zone_chip_enabled_text else R.color.zone_chip_disabled_text
            )
        )

        modeChip.background = view.context.getDrawable(R.drawable.bg_zone_chip_neutral)
        modeChip.setTextColor(ContextCompat.getColor(view.context, R.color.zone_chip_neutral_text))

    }
}

private class SearchResultInfoWindow(
    mapView: MapView,
    private val onAdd: (String, Double, Double) -> Unit
) : MarkerInfoWindow(R.layout.view_search_result_popup, mapView) {

    init {
        mView.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onOpen(item: Any?) {
        val marker = item as? Marker ?: return
        val view = mView
        
        val nameText = view.findViewById<android.widget.TextView>(R.id.location_name)
        val addButton = view.findViewById<android.widget.TextView>(R.id.btn_add_zone)
        
        val label = marker.title ?: ""
        nameText.text = label
        
        addButton.setOnClickListener {
            onAdd(label, marker.position.latitude, marker.position.longitude)
            close()
        }
    }
}
