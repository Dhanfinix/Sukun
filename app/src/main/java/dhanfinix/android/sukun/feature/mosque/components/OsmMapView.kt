package dhanfinix.android.sukun.feature.mosque.components

import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import dhanfinix.android.sukun.core.database.entity.MosqueLocation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import android.graphics.drawable.Drawable
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import android.graphics.Color as AndroidColor

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    locations: List<MosqueLocation> = emptyList(),
    userLat: Double? = null,
    userLng: Double? = null,
    centerLat: Double? = null,
    centerLng: Double? = null,
    zoom: Double = 15.0,
    onMapClick: () -> Unit = {},
    onMapLongClick: (Double, Double) -> Unit = { _, _ -> },
    onMarkerClick: (MosqueLocation) -> Unit = {},
    onCenterChanged: (Double, Double) -> Unit = { _, _ -> },
    onMyLocationUpdate: (Double, Double) -> Unit = { _, _ -> }
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
                    title = "My Location"
                    
                    // Programmatically create a blue dot icon
                    val size = (16 * mv.context.resources.displayMetrics.density).toInt()
                    val strokeWidth = (2 * mv.context.resources.displayMetrics.density).toInt()
                    
                    val shape = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(android.graphics.Color.parseColor("#2196F3"))
                        setStroke(strokeWidth, android.graphics.Color.WHITE)
                        setSize(size, size)
                    }
                    icon = shape
                    setInfoWindow(null) // Don't show info window for user location
                }
                mv.overlays.add(userMarker)
            }

            // Update Mosque Markers
            mv.overlays.removeAll { it is Marker && it.id != "user_location" }
            locations.forEach { mosque ->
                val marker = Marker(mv).apply {
                    id = mosque.id.toString()
                    position = GeoPoint(mosque.latitude, mosque.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = mosque.name
                    snippet = mosque.address
                    
                    // Tint the default icon green
                    val mosqueIcon = icon.constantState?.newDrawable()?.mutate()
                    mosqueIcon?.setColorFilter(android.graphics.Color.parseColor("#4CAF50"), PorterDuff.Mode.SRC_IN)
                    icon = mosqueIcon

                    setOnMarkerClickListener { m, _ ->
                        onMarkerClick(mosque)
                        m.showInfoWindow()
                        true
                    }
                }
                mv.overlays.add(marker)
            }

            // Sync Center if changed from outside
            if (centerLat != null && centerLng != null) {
                val currentCenter = mv.mapCenter as GeoPoint
                if (Math.abs(currentCenter.latitude - centerLat) > 0.00001 ||
                    Math.abs(currentCenter.longitude - centerLng) > 0.00001
                ) {
                    mv.controller.animateTo(GeoPoint(centerLat, centerLng))
                }
            }
            
            mv.invalidate()
        }
    )

    // Listener for map center changes (to update mapCenter state in parent)
    DisposableEffect(mapView) {
        val listener = object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                val center = mapView.mapCenter as GeoPoint
                onCenterChanged(center.latitude, center.longitude)
                return true
            }

            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                return true
            }
        }
        mapView.addMapListener(listener)
        onDispose {
            mapView.removeMapListener(listener)
        }
    }
}
