package com.example.assignment3_cos30017.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.databinding.ActivityMapBinding
import com.example.assignment3_cos30017.ui.detail.DetailActivity
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.viewmodel.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.abs
import kotlin.math.cos

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val EXTRA_NEAR_ME = "extra_near_me"
        const val EXTRA_FOCUS_CAR_ID = "extra_focus_car_id"
        private const val NEAR_ME_RADIUS_KM = 10.0

        fun startNearMe(context: Context) {
            context.startActivity(Intent(context, MapActivity::class.java).apply {
                putExtra(EXTRA_NEAR_ME, true)
            })
        }
    }

    private lateinit var binding: ActivityMapBinding
    private val viewModel: MapViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private val markerCarMap = mutableMapOf<Marker, Car>()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val isNearMe by lazy { intent.getBooleanExtra(EXTRA_NEAR_ME, false) }
    private val focusCarId by lazy { intent.getStringExtra(EXTRA_FOCUS_CAR_ID).orEmpty() }
    private var userLocation: LatLng? = null
    private var userMarker: Marker? = null
    private var allCars: List<Car> = emptyList()
    private var currentRoutePolyline: Polyline? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (isNearMe || focusCarId.isNotBlank()) fetchUserLocationAndFilter()
            else renderCars(allCars)
        } else {
            renderCars(allCars)
            if (focusCarId.isNotBlank()) {
                allCars.firstOrNull { it.id == focusCarId }?.let { showCarBottomSheet(it) }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyInsetsToBottomSheet()

        binding.toolbar.setNavigationOnClickListener { finish() }
        if (isNearMe) binding.toolbar.title = getString(R.string.near_me)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        if (isNearMe) setupLegend()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun applyInsetsToBottomSheet() {
        val sheet = binding.bottomSheet
        val startPaddingBottom = sheet.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(sheet) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, startPaddingBottom + bottom)
            insets
        }
    }

    private fun setupLegend() {
        binding.legendLayout.visibility = View.VISIBLE
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-37.8136, 144.9631), 12f))

        map.setOnMarkerClickListener { marker ->
            markerCarMap[marker]?.let { showCarBottomSheet(it) }
            true
        }
        map.setOnMapClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            currentRoutePolyline?.remove()
            currentRoutePolyline = null
        }

        viewModel.cars.observe(this) { cars ->
            allCars = cars
            if (isNearMe || focusCarId.isNotBlank()) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    fetchUserLocationAndFilter()
                } else {
                    locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } else {
                renderCars(cars)
            }
        }
    }

    private fun fetchUserLocationAndFilter() {
        try {
            LocationServices.getFusedLocationProviderClient(this)
                .lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        userLocation = userLatLng
                        if (isNearMe) googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f))

                        if (isNearMe) {
                            // Fast pre-filter (bounding box) before running distanceBetween on every car.
                            val latDelta = NEAR_ME_RADIUS_KM / 111.0
                            val lngDelta = NEAR_ME_RADIUS_KM / (111.0 * cos(Math.toRadians(userLatLng.latitude)).coerceAtLeast(1e-6))

                            val nearby = allCars.asSequence()
                                .filter { car -> car.latitude != 0.0 && car.longitude != 0.0 }
                                .filter { car ->
                                    abs(car.latitude - userLatLng.latitude) <= latDelta &&
                                        abs(car.longitude - userLatLng.longitude) <= lngDelta
                                }
                                .filter { car ->
                                    val results = FloatArray(1)
                                    Location.distanceBetween(
                                        location.latitude, location.longitude,
                                        car.latitude, car.longitude, results
                                    )
                                    results[0] / 1000.0 <= NEAR_ME_RADIUS_KM
                                }
                                .toList()

                            renderCars(nearby)
                        } else {
                            renderCars(allCars)
                        }

                        if (focusCarId.isNotBlank()) {
                            allCars.firstOrNull { it.id == focusCarId }?.let { target ->
                                googleMap?.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(target.latitude, target.longitude),
                                        14.5f
                                    )
                                )
                                showCarBottomSheet(target)
                            }
                        }
                    } else {
                        renderCars(allCars)
                        if (focusCarId.isNotBlank()) {
                            allCars.firstOrNull { it.id == focusCarId }?.let { showCarBottomSheet(it) }
                        }
                    }
                }
        } catch (_: SecurityException) {
            renderCars(allCars)
            if (focusCarId.isNotBlank()) {
                allCars.firstOrNull { it.id == focusCarId }?.let { showCarBottomSheet(it) }
            }
        }
    }

    private fun drawRadiusCircle(center: LatLng) {
        googleMap?.addCircle(
            CircleOptions()
                .center(center)
                .radius(NEAR_ME_RADIUS_KM * 1000)
                .strokeColor(0x552383E2.toInt())
                .fillColor(0x112383E2.toInt())
                .strokeWidth(2f)
        )
    }

    private fun renderCars(cars: List<Car>) {
        val map = googleMap ?: return
        map.clear()
        markerCarMap.clear()
        userLocation?.let { loc ->
            drawRadiusCircle(loc)
            addUserMarker(loc)
        }
        cars.filter { it.latitude != 0.0 && it.longitude != 0.0 }.forEach { car ->
            val isOwnCar = car.ownerId == currentUserId
            val hue = when {
                isOwnCar -> BitmapDescriptorFactory.HUE_AZURE
                else -> BitmapDescriptorFactory.HUE_GREEN
            }
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(car.latitude, car.longitude))
                    .title(car.title).snippet(car.locationName)
                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
            )
            if (marker != null) markerCarMap[marker] = car
        }
    }

    private fun addUserMarker(latLng: LatLng) {
        userMarker = googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(getString(R.string.my_location))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .zIndex(1f)
        )
    }

    private fun showCarBottomSheet(car: Car) {
        // Remove previous route
        currentRoutePolyline?.remove()
        currentRoutePolyline = null

        if (car.imageUrls.isNotEmpty()) {
            Glide.with(this).load(car.imageUrls.first()).into(binding.ivSheetCar)
        }
        binding.tvSheetName.text = car.title
        binding.tvSheetCost.text = getString(R.string.credits_per_day_format, car.dailyCost)
        binding.tvSheetLocation.text = car.locationName
        binding.tvSheetMaxDays.text = getString(R.string.max_rental_days_format, car.maxRentalDays)

        binding.btnSheetClose.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            currentRoutePolyline?.remove()
            currentRoutePolyline = null
        }

        // Show distance if user location is known
        val userLoc = userLocation
        val carLatLng = LatLng(car.latitude, car.longitude)
        if (userLoc != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLoc.latitude, userLoc.longitude,
                car.latitude, car.longitude, results
            )
            val distanceKm = results[0] / 1000.0
            binding.tvSheetDistance.text = if (distanceKm < 1.0) {
                getString(R.string.distance_meters_format, results[0].toInt())
            } else {
                getString(R.string.distance_km_format, distanceKm)
            }
            binding.tvSheetDistance.visibility = View.VISIBLE

            // Straight-line indicator (no paid Directions API).
            val map = googleMap
            if (map != null) {
                val polylineOptions = PolylineOptions()
                    .add(userLoc, carLatLng)
                    .width(6f)
                    .color(ContextCompat.getColor(this@MapActivity, R.color.accent))
                    .geodesic(true)
                currentRoutePolyline?.remove()
                currentRoutePolyline = map.addPolyline(polylineOptions)
            }
        } else {
            binding.tvSheetDistance.visibility = View.GONE
        }

        binding.btnSheetDetails.setOnClickListener {
            startActivity(Intent(this, DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_CAR_ID, car.id)
            })
        }
        binding.bottomSheet.visibility = View.VISIBLE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}
