package com.example.assignment3_cos30017.ui.listcar

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.repository.CarRepository
import com.example.assignment3_cos30017.databinding.ActivityListCarBinding
import com.example.assignment3_cos30017.ui.map.MapPickerActivity
import com.example.assignment3_cos30017.util.LocaleHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale

class ListCarActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val EXTRA_CAR_ID = "extra_car_id"
        const val EXTRA_EDIT_MODE = "extra_edit_mode"
    }

    private lateinit var binding: ActivityListCarBinding
    private val carRepository = CarRepository()
    private var googleMap: GoogleMap? = null
    private var selectedLatLng: LatLng? = null
    private var selectedLocationName = ""
    private var isEditMode = false

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) moveToUserLocation()
    }

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val lat = data.getDoubleExtra(MapPickerActivity.EXTRA_LATITUDE, 0.0)
            val lng = data.getDoubleExtra(MapPickerActivity.EXTRA_LONGITUDE, 0.0)
            val name = data.getStringExtra(MapPickerActivity.EXTRA_LOCATION_NAME) ?: ""
            val latLng = LatLng(lat, lng)
            selectedLatLng = latLng
            selectedLocationName = name
            binding.tvLocationName.text = name
            binding.tvLocationError.visibility = View.GONE
            googleMap?.let { map ->
                map.clear()
                map.addMarker(MarkerOptions().position(latLng))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListCarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val carId = intent.getStringExtra(EXTRA_CAR_ID) ?: run { finish(); return }
        isEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)

        if (isEditMode) {
            binding.toolbar.title = getString(R.string.edit_listing_title)
            binding.btnSubmit.text = getString(R.string.update_listing)
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        loadCarSummary(carId)

        binding.root.post {
            val mapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.map_picker, mapFragment)
                .commitAllowingStateLoss()
            mapFragment.getMapAsync(this)
        }

        binding.btnSubmit.setOnClickListener { submit(carId) }
        binding.btnOpenFullMap.setOnClickListener { openFullMapPicker() }
    }

    private fun openFullMapPicker() {
        val intent = Intent(this, MapPickerActivity::class.java)
        selectedLatLng?.let {
            intent.putExtra(MapPickerActivity.EXTRA_INITIAL_LAT, it.latitude)
            intent.putExtra(MapPickerActivity.EXTRA_INITIAL_LNG, it.longitude)
        }
        mapPickerLauncher.launch(intent)
    }

    private fun loadCarSummary(carId: String) {
        setLoading(true)
        lifecycleScope.launch {
            val car = carRepository.getCarById(carId).firstOrNull() ?: run {
                setLoading(false); return@launch
            }
            binding.tvCarTitle.text = car.title
            binding.tvCarModelYear.text = getString(R.string.model_year_format, car.model, car.year)
            if (car.imageUrls.isNotEmpty()) {
                Glide.with(this@ListCarActivity)
                    .load(car.imageUrls.first())
                    .placeholder(R.drawable.ic_car_logo)
                    .into(binding.ivCarThumb)
            }
            // Pre-fill listing terms in edit mode
            if (isEditMode) {
                if (car.dailyCost > 0) binding.etDailyCost.setText(car.dailyCost.toString())
                if (car.maxRentalDays > 0) binding.etMaxDays.setText(car.maxRentalDays.toString())
                if (car.notes.isNotBlank()) binding.etNotes.setText(car.notes)
            }
            // Pre-fill location if car already has one
            if (car.latitude != 0.0 || car.longitude != 0.0) {
                selectedLatLng = LatLng(car.latitude, car.longitude)
                selectedLocationName = car.locationName
                binding.tvLocationName.text = car.locationName
                googleMap?.let { map ->
                    map.clear()
                    map.addMarker(MarkerOptions().position(selectedLatLng!!))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng!!, 14f))
                }
            }
            setLoading(false)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        binding.mapLoadingPlaceholder.visibility = View.GONE
        val existingLoc = selectedLatLng
        if (existingLoc != null) {
            map.clear()
            map.addMarker(MarkerOptions().position(existingLoc))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(existingLoc, 14f))
        } else {
            val defaultLocation = LatLng(-37.8136, 144.9631)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                moveToUserLocation()
            } else {
                locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        map.setOnMapClickListener { latLng ->
            map.clear()
            map.addMarker(MarkerOptions().position(latLng))
            selectedLatLng = latLng
            binding.tvLocationError.visibility = View.GONE
            reverseGeocode(latLng)
        }
    }

    private fun moveToUserLocation() {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
                }
            }
        } catch (_: SecurityException) { }
    }

    private fun reverseGeocode(latLng: LatLng) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            selectedLocationName = addresses?.firstOrNull()?.let { addr ->
                // Use full formatted address line if available
                if (addr.maxAddressLineIndex >= 0) {
                    addr.getAddressLine(0)
                } else {
                    listOfNotNull(addr.thoroughfare, addr.subLocality, addr.locality, addr.adminArea)
                        .joinToString(", ")
                        .ifBlank { "Lat: %.4f, Lng: %.4f".format(latLng.latitude, latLng.longitude) }
                }
            } ?: "Lat: %.4f, Lng: %.4f".format(latLng.latitude, latLng.longitude)
            binding.tvLocationName.text = selectedLocationName
        } catch (e: Exception) {
            selectedLocationName = "Lat: %.4f, Lng: %.4f".format(latLng.latitude, latLng.longitude)
            binding.tvLocationName.text = selectedLocationName
        }
    }

    private fun submit(carId: String) {
        if (!binding.btnSubmit.isEnabled) return
        binding.btnSubmit.isEnabled = false

        val dailyCost = binding.etDailyCost.text?.toString()?.toIntOrNull() ?: 0
        val maxDays = binding.etMaxDays.text?.toString()?.toIntOrNull() ?: 0
        val notes = binding.etNotes.text?.toString()?.trim() ?: ""

        var valid = true
        if (dailyCost <= 0) {
            binding.tilDailyCost.error = getString(R.string.error_daily_cost_zero); valid = false
        } else binding.tilDailyCost.error = null

        if (maxDays <= 0) {
            binding.tilMaxDays.error = getString(R.string.error_max_days_zero); valid = false
        } else binding.tilMaxDays.error = null

        if (selectedLatLng == null) {
            binding.tvLocationError.text = getString(R.string.error_select_location)
            binding.tvLocationError.visibility = View.VISIBLE
            valid = false
        } else {
            binding.tvLocationError.visibility = View.GONE
        }

        if (!valid) { binding.btnSubmit.isEnabled = true; return }

        setLoading(true)
        lifecycleScope.launch {
            try {
                carRepository.listCar(
                    carId, dailyCost, maxDays, notes,
                    selectedLatLng!!.latitude, selectedLatLng!!.longitude, selectedLocationName
                )
                val msgRes = if (isEditMode) R.string.listing_updated_success else R.string.list_car_success
                Snackbar.make(binding.root, msgRes, Snackbar.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Snackbar.make(binding.root, e.localizedMessage ?: getString(R.string.generic_error), Snackbar.LENGTH_SHORT).show()
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !loading
    }
}
