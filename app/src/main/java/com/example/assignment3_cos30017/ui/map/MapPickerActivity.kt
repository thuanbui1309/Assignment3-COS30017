package com.example.assignment3_cos30017.ui.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.databinding.ActivityMapPickerBinding
import com.example.assignment3_cos30017.util.LocaleHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_LOCATION_NAME = "extra_location_name"
        const val EXTRA_INITIAL_LAT = "extra_initial_lat"
        const val EXTRA_INITIAL_LNG = "extra_initial_lng"
    }

    private lateinit var binding: ActivityMapPickerBinding
    private var googleMap: GoogleMap? = null
    private var selectedAddress = ""

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) moveToUserLocation()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation(binding.etSearch.text?.toString() ?: "")
                true
            } else false
        }

        binding.btnConfirm.setOnClickListener {
            val center = googleMap?.cameraPosition?.target ?: return@setOnClickListener
            val resultIntent = Intent().apply {
                putExtra(EXTRA_LATITUDE, center.latitude)
                putExtra(EXTRA_LONGITUDE, center.longitude)
                putExtra(EXTRA_LOCATION_NAME, selectedAddress)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val initialLat = intent.getDoubleExtra(EXTRA_INITIAL_LAT, 0.0)
        val initialLng = intent.getDoubleExtra(EXTRA_INITIAL_LNG, 0.0)

        if (initialLat != 0.0 || initialLng != 0.0) {
            val initial = LatLng(initialLat, initialLng)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(initial, 15f))
            reverseGeocode(initial)
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-37.8136, 144.9631), 12f))
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                moveToUserLocation()
            } else {
                locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        map.setOnCameraIdleListener {
            val center = map.cameraPosition.target
            reverseGeocode(center)
        }
    }

    private fun moveToUserLocation() {
        try {
            LocationServices.getFusedLocationProviderClient(this)
                .lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    }
                }
        } catch (_: SecurityException) { }
    }

    private fun searchLocation(query: String) {
        if (query.isBlank()) return
        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)

        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val results = geocoder.getFromLocationName(query, 5)
            if (!results.isNullOrEmpty()) {
                val addr = results.first()
                val latLng = LatLng(addr.latitude, addr.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Snackbar.make(binding.root, R.string.no_results_found, Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, R.string.no_results_found, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun reverseGeocode(latLng: LatLng) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            selectedAddress = addresses?.firstOrNull()?.let { addr ->
                if (addr.maxAddressLineIndex >= 0) {
                    addr.getAddressLine(0)
                } else {
                    listOfNotNull(addr.thoroughfare, addr.subLocality, addr.locality, addr.adminArea)
                        .joinToString(", ")
                        .ifBlank { "Lat: %.4f, Lng: %.4f".format(latLng.latitude, latLng.longitude) }
                }
            } ?: "Lat: %.4f, Lng: %.4f".format(latLng.latitude, latLng.longitude)
            binding.tvSelectedAddress.text = selectedAddress
        } catch (e: Exception) {
            selectedAddress = "Lat: %.4f, Lng: %.4f".format(latLng.latitude, latLng.longitude)
            binding.tvSelectedAddress.text = selectedAddress
        }
    }
}
