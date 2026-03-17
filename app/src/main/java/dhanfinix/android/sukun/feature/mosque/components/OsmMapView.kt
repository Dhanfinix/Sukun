package dhanfinix.android.sukun.feature.mosque.components

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

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    zones: List<SilentZone> = emptyList(),
    userLat: Double? = null,
    userLng: Double? = null,
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
    onZoomChanged: (Double) -> Unit = {}
) {
    val context = LocalContext.current
    
    // OSM configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm_pref", 0))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(true)
        }
    }
    
    val zoneInfoWindow = remember(mapView) { SilentZoneInfoWindow(mapView) }

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
        if (Math.abs(mapView.zoomLevelDouble - zoom) > 0.1) {
            mapView.controller.setZoom(zoom)
        }
    }
    
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            mapView.apply {
                overlays.add(clickOverlay)
                controller.setZoom(zoom)
                centerLat?.let { lat ->
                    centerLng?.let { lng ->
                        controller.setCenter(GeoPoint(lat, lng))
                    }
                }
            }
        },
        update = { mv ->
            // Update User Location Marker
            mv.overlays.removeAll { it is Marker && it.id == "user_location" }
            if (userLat != null && userLng != null) {
                val userMarker = Marker(mv).apply {
                    id = "user_location"
                    position = GeoPoint(userLat, userLng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = mv.context.getString(R.string.label_my_location)
                    
                    val size = (16 * mv.context.resources.displayMetrics.density).toInt()
                    val strokeWidth = (2 * mv.context.resources.displayMetrics.density).toInt()
                    
                    val shape = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor("#2196F3".toColorInt())
                        setStroke(strokeWidth, android.graphics.Color.WHITE)
                        setSize(size, size)
                    }
                    icon = shape
                    setInfoWindow(null)
                }
                mv.overlays.add(userMarker)
            }

            // Update Silent Zone Radii (Polygons)
            mv.overlays.removeAll { it is Polygon }
            zones.forEach { zone ->
                val circle = Polygon(mv).apply {
                    id = "poly_${zone.id}"
                    points = Polygon.pointsAsCircle(GeoPoint(zone.latitude, zone.longitude), zone.radius.toDouble())
                    
                    val colorHex = if (zone.isEnabled) "#4CAF50" else "#9E9E9E"
                    val fillColorHex = if (zone.isEnabled) "#334CAF50" else "#339E9E9E"
                    
                    fillPaint.color = fillColorHex.toColorInt()
                    outlinePaint.color = colorHex.toColorInt()
                    outlinePaint.strokeWidth = 3f * mv.context.resources.displayMetrics.density
                }
                circle.setOnClickListener { _, _, _ ->
                    onMarkerClick(zone)
                    true
                }
                mv.overlays.add(circle)
            }

            // Update Silent Zone Markers
            mv.overlays.removeAll { it is Marker && it.id != "user_location" }
            zones.forEach { zone ->
                val marker = Marker(mv).apply {
                    id = zone.id.toString()
                    position = GeoPoint(zone.latitude, zone.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = zone.name
                    relatedObject = zone
                    infoWindow = zoneInfoWindow
                    
                    val density = mv.context.resources.displayMetrics.density
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
                mv.overlays.add(marker)
            }

            if (searchLat != null && searchLng != null) {
                val searchMarker = Marker(mv).apply {
                    id = "search_location"
                    position = GeoPoint(searchLat, searchLng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = searchLabel ?: mv.context.getString(R.string.label_search_result)
                    icon = ContextCompat.getDrawable(mv.context, R.drawable.ic_search_pin)
                }
                mv.overlays.add(searchMarker)
            }

            if (focusedZoneId == null) {
                InfoWindow.closeAllInfoWindowsOn(mv)
            } else {
                val focusedMarker = mv.overlays
                    .filterIsInstance<Marker>()
                    .firstOrNull { it.id == focusedZoneId.toString() }
                if (focusedMarker != null) {
                    InfoWindow.closeAllInfoWindowsOn(mv)
                    focusedMarker.showInfoWindow()
                }
            }

            // Sync Center
            if (centerLat != null && centerLng != null) {
                val currentCenter = mv.mapCenter as GeoPoint
                if (Math.abs(currentCenter.latitude - centerLat) > 0.0001 ||
                    Math.abs(currentCenter.longitude - centerLng) > 0.0001
                ) {
                    mv.controller.animateTo(GeoPoint(centerLat, centerLng))
                    // If we are centering, we usually want to ensure the zoom is also correct
                    if (Math.abs(mv.zoomLevelDouble - zoom) > 0.1) {
                        mv.controller.zoomTo(zoom)
                    }
                }
            }
            
            mv.invalidate()
        }
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
        radius.text = view.resources.getString(
            R.string.radius_value_format,
            zone.radius.toInt()
        )

        val durationMinutes = zone.silenceDuration ?: 30
        duration.text = if (zone.isAutoSilent) {
            if (durationMinutes >= 60) {
                view.resources.getString(R.string.ends_in_hours, durationMinutes / 60)
            } else {
                view.resources.getString(R.string.ends_in_mins, durationMinutes)
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
