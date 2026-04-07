package com.example.assignment3_cos30017.ui.detail

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment3_cos30017.BuildConfig
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Bid
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.databinding.ActivityDetailBinding
import com.example.assignment3_cos30017.databinding.DialogPlaceBidBinding
import com.example.assignment3_cos30017.ui.adapter.BidAdapter
import com.example.assignment3_cos30017.ui.chat.ChatActivity
import com.example.assignment3_cos30017.ui.map.MapActivity
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.viewmodel.ChatViewModel
import com.example.assignment3_cos30017.viewmodel.DetailUiState
import com.example.assignment3_cos30017.viewmodel.DetailViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val EXTRA_CAR_ID = DetailViewModel.EXTRA_CAR_ID
        const val EXTRA_ENTRY_CONTEXT = DetailViewModel.EXTRA_ENTRY_CONTEXT
        const val EXTRA_BOOKING_ID = DetailViewModel.EXTRA_BOOKING_ID
        private const val BID_PAGE_SIZE = 5
    }

    private lateinit var binding: ActivityDetailBinding
    private val viewModel: DetailViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private var bidAdapter: BidAdapter? = null
    private var allSortedBids: List<Bid> = emptyList()
    private var visibleBidCount = BID_PAGE_SIZE

    private var googleMap: GoogleMap? = null
    private var carMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var routeJob: Job? = null
    private var lastCarLatLng: LatLng? = null
    private var userLatLng: LatLng? = null

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchUserLocationAndRenderRoute()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        initMapPreview()
        viewModel.load()
        observeState()
        observeActions()
    }

    private fun initMapPreview() {
        // Map in a ScrollView is fine as long as we keep it non-interactive.
        val frag = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_preview_container, frag)
            .commitAllowingStateLoss()
        frag.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.apply {
            isMapToolbarEnabled = false
            isZoomControlsEnabled = false
            isMyLocationButtonEnabled = false
            isScrollGesturesEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
        }
        // Default camera (Melbourne)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-37.8136, 144.9631), 11f))
        // If state already bound a car location, render it now.
        renderCarLocationOnMap()
    }

    private fun observeState() {
        viewModel.uiState.observe(this) { state ->
            hideAll()
            when (state) {
                is DetailUiState.Loading -> {}
                is DetailUiState.RenterBrowsing -> renderRenterBrowsing(state)
                is DetailUiState.RenterAlreadyBid -> renderRenterAlreadyBid(state)
                is DetailUiState.MarketplaceRented -> renderMarketplaceRented(state)
                is DetailUiState.OwnerMarketplace -> renderOwnerMarketplace(state)
                is DetailUiState.OwnerGarage -> renderOwnerGarage(state)
                is DetailUiState.OwnerListed -> renderOwnerListed(state)
                is DetailUiState.OwnerRented -> renderOwnerRented(state)
                is DetailUiState.RenterActiveBooking -> renderRenterActiveBooking(state)
                is DetailUiState.RenterBookingHistory -> renderRenterBookingHistory(state)
            }
        }
    }

    private fun observeActions() {
        viewModel.actionResult.observe(this) { result ->
            result ?: return@observe
            setLoading(false)
            viewModel.clearActionResult()
            when (result) {
                is DetailViewModel.ActionResult.CarListed ->
                    snack(R.string.car_listed)
                is DetailViewModel.ActionResult.CarUnlisted ->
                    snack(R.string.car_unlisted)
                is DetailViewModel.ActionResult.CarDeleted -> {
                    snack(R.string.car_deleted); finish()
                }
                is DetailViewModel.ActionResult.BookingCompleted ->
                    snack(R.string.booking_completed)
                is DetailViewModel.ActionResult.BookingCancelled ->
                    snack(R.string.booking_cancelled)
                is DetailViewModel.ActionResult.BidPlaced ->
                    snack(R.string.bid_placed)
                is DetailViewModel.ActionResult.BidCancelled ->
                    snack(R.string.bid_cancelled)
                is DetailViewModel.ActionResult.BidApproved ->
                    snack(R.string.bid_approved)
                is DetailViewModel.ActionResult.BidRejected ->
                    snack(R.string.bid_rejected)
                is DetailViewModel.ActionResult.BidUpdated ->
                    snack(R.string.bid_updated)
                is DetailViewModel.ActionResult.Error ->
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
                is DetailViewModel.ActionResult.ErrorRes ->
                    Snackbar.make(binding.root, getString(result.resId), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideAll() {
        binding.tvDescription.visibility = View.GONE
        binding.layoutLocationMap.visibility = View.GONE
        binding.layoutBalance.visibility = View.GONE
        binding.layoutBookingInfo.visibility = View.GONE
        binding.layoutMyBid.visibility = View.GONE
        binding.layoutBids.visibility = View.GONE
        binding.tvStatusBanner.visibility = View.GONE
        binding.tvOwnerName.visibility = View.GONE
        binding.btnPlaceBid.visibility = View.GONE
        binding.btnChatOwner.visibility = View.GONE
        binding.btnListForRent.visibility = View.GONE
        binding.btnUnlist.visibility = View.GONE
        binding.btnEdit.visibility = View.GONE
        binding.btnEditListing.visibility = View.GONE
        binding.btnDelete.visibility = View.GONE
        binding.btnMarkComplete.visibility = View.GONE
        binding.btnCancelBooking.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
    }

    // --- Render Methods ---

    private fun renderRenterBrowsing(state: DetailUiState.RenterBrowsing) {
        bindCarInfo(state.car)
        showOwnerName(state.car)
        binding.layoutBalance.visibility = View.VISIBLE
        binding.tvBalance.text = getString(R.string.credits_format, state.balance)
        binding.btnPlaceBid.visibility = View.VISIBLE
        binding.btnChatOwner.visibility = View.VISIBLE
        binding.btnPlaceBid.setOnClickListener { showBidDialog(state.car, state.balance) }
        binding.btnChatOwner.setOnClickListener { openChat(state.car.ownerId, state.car.ownerName) }

        showBidListForRenter(state.bids)
    }

    private fun renderRenterAlreadyBid(state: DetailUiState.RenterAlreadyBid) {
        bindCarInfo(state.car)
        showOwnerName(state.car)
        binding.layoutMyBid.visibility = View.VISIBLE
        binding.tvMyBidDetails.text = getString(
            R.string.bid_summary_format, state.myBid.dailyRate, state.myBid.rentalDays
        ) + "\n" + getString(R.string.credits_format, state.myBid.totalAmount)
        binding.btnChatOwner.visibility = View.VISIBLE
        binding.btnChatOwner.setOnClickListener { openChat(state.car.ownerId, state.car.ownerName) }

        // Allow editing pending bid
        binding.btnPlaceBid.visibility = View.VISIBLE
        binding.btnPlaceBid.text = getString(R.string.edit_bid)
        binding.btnPlaceBid.setOnClickListener { showBidDialog(state.car, balance = state.balance, existingBid = state.myBid) }

        showBidListForRenter(state.bids)
    }

    private fun renderMarketplaceRented(state: DetailUiState.MarketplaceRented) {
        bindCarInfo(state.car)
        showStatusBanner(getString(R.string.currently_rented), R.color.error)
    }

    private fun renderOwnerMarketplace(state: DetailUiState.OwnerMarketplace) {
        bindCarInfo(state.car)
        val pendingCount = state.bids.count { it.status == Bid.STATUS_PENDING }
        if (pendingCount > 0) {
            showStatusBanner(getString(R.string.bids_count, pendingCount), R.color.accent)
        }
        binding.btnUnlist.visibility = View.VISIBLE
        binding.btnEditListing.visibility = View.VISIBLE
        binding.btnUnlist.setOnClickListener { confirmUnlist() }
        binding.btnEditListing.setOnClickListener { openEditListing(state.car.id) }

        showBidListForOwner(state.bids)
    }

    private fun renderOwnerGarage(state: DetailUiState.OwnerGarage) {
        bindCarInfo(state.car)
        binding.btnListForRent.visibility = View.VISIBLE
        binding.btnEdit.visibility = View.VISIBLE
        binding.btnDelete.visibility = View.VISIBLE
        binding.btnListForRent.setOnClickListener {
            startActivity(Intent(this, com.example.assignment3_cos30017.ui.listcar.ListCarActivity::class.java).apply {
                putExtra(com.example.assignment3_cos30017.ui.listcar.ListCarActivity.EXTRA_CAR_ID, state.car.id)
            })
        }
        binding.btnEdit.setOnClickListener { openEditCar(state.car.id) }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun renderOwnerListed(state: DetailUiState.OwnerListed) {
        bindCarInfo(state.car)
        binding.btnUnlist.visibility = View.VISIBLE
        binding.btnEditListing.visibility = View.VISIBLE
        binding.btnUnlist.setOnClickListener { confirmUnlist() }
        binding.btnEditListing.setOnClickListener { openEditListing(state.car.id) }

        showBidListForOwner(state.bids)
    }

    private fun updateBidPagination() {
        bidAdapter?.submitList(allSortedBids.take(visibleBidCount))
        binding.btnLoadMoreBids?.visibility =
            if (allSortedBids.size > visibleBidCount) View.VISIBLE else View.GONE
    }

    private fun showBidListForOwner(bids: List<Bid>) {
        binding.layoutBids.visibility = View.VISIBLE
        val pendingBids = bids.filter { it.status == Bid.STATUS_PENDING }
        binding.tvBidsTitle.text = getString(R.string.bids_count, pendingBids.size)
        binding.tvNoBids.visibility = if (pendingBids.isEmpty()) View.VISIBLE else View.GONE

        allSortedBids = bids.sortedByDescending { it.totalAmount }
        visibleBidCount = BID_PAGE_SIZE

        bidAdapter = BidAdapter(
            showActions = true,
            showRejectButton = true,
            onApprove = { bid -> confirmApproveBid(bid) },
            onReject = { bid -> confirmRejectBid(bid) },
            onChat = { bid -> openChat(bid.bidderId, bid.bidderName) }
        )
        binding.rvBids.layoutManager = LinearLayoutManager(this)
        binding.rvBids.adapter = bidAdapter
        updateBidPagination()

        binding.btnLoadMoreBids?.setOnClickListener {
            visibleBidCount += BID_PAGE_SIZE
            updateBidPagination()
        }
    }

    private fun showBidListForRenter(bids: List<Bid>) {
        binding.layoutBids.visibility = View.VISIBLE

        val pendingBids = bids.filter { it.status == Bid.STATUS_PENDING }
        binding.tvBidsTitle.text = getString(R.string.bids_count, pendingBids.size)
        binding.tvNoBids.visibility = if (pendingBids.isEmpty()) View.VISIBLE else View.GONE

        allSortedBids = pendingBids.sortedByDescending { it.totalAmount }
        visibleBidCount = BID_PAGE_SIZE

        bidAdapter = BidAdapter(
            showActions = false,
            showRejectButton = false,
            onApprove = { },
            onReject = { },
            onChat = { bid -> openChat(bid.bidderId, bid.bidderName) }
        )
        binding.rvBids.layoutManager = LinearLayoutManager(this)
        binding.rvBids.adapter = bidAdapter
        updateBidPagination()

        binding.btnLoadMoreBids?.setOnClickListener {
            visibleBidCount += BID_PAGE_SIZE
            updateBidPagination()
        }
    }

    private fun renderOwnerRented(state: DetailUiState.OwnerRented) {
        bindCarInfo(state.car)
        showStatusBanner(getString(R.string.status_rented), R.color.error)
        binding.layoutBookingInfo.visibility = View.VISIBLE
        binding.tvBookingDetails.text = getString(
            R.string.booking_details_format,
            state.booking.renterName,
            state.booking.rentalDays,
            state.booking.totalCost
        )
    }

    private fun renderRenterActiveBooking(state: DetailUiState.RenterActiveBooking) {
        bindCarInfo(state.car)
        showOwnerName(state.car)
        binding.layoutBookingInfo.visibility = View.VISIBLE
        binding.tvBookingDetails.text = getString(
            R.string.booking_days_format, state.booking.rentalDays, state.booking.totalCost
        )
        binding.btnCancelBooking.visibility = View.VISIBLE
        binding.btnCancelBooking.setOnClickListener { viewModel.cancelBooking() }
    }

    private fun renderRenterBookingHistory(state: DetailUiState.RenterBookingHistory) {
        bindCarInfo(state.car)
        showOwnerName(state.car)
        binding.layoutBookingInfo.visibility = View.VISIBLE
        val statusLabel = when (state.booking.status) {
            "COMPLETED" -> getString(R.string.status_completed)
            "CANCELLED" -> getString(R.string.status_cancelled)
            else -> state.booking.status
        }
        binding.tvBookingDetails.text = getString(
            R.string.booking_days_format, state.booking.rentalDays, state.booking.totalCost
        ) + " \u2022 $statusLabel"
    }

    // --- Helpers ---

    private val imageSliderAdapter = com.example.assignment3_cos30017.ui.adapter.ImageSliderAdapter()

    private fun bindCarInfo(car: Car) {
        binding.vpImages.adapter = imageSliderAdapter
        binding.vpImages.offscreenPageLimit = 2
        imageSliderAdapter.submitList(car.imageUrls)
        if (BuildConfig.IMAGE_PRELOAD_ALL) {
            imageSliderAdapter.preloadAll(this)
        } else {
            imageSliderAdapter.preloadAround(this, centerIndex = 0, radius = 1)
        }
        setupDots(car.imageUrls.size)

        binding.tvCarName.text = car.title
        binding.tvModelYear.visibility = View.GONE
        binding.tvSpecKmValue.text = car.model
        binding.tvSpecYearValue.text = car.year.toString()

        if (car.dailyCost > 0) {
            binding.layoutSpecCost.visibility = View.VISIBLE
            binding.tvSpecCostValue.text = getString(R.string.credits_per_day_format, car.dailyCost)
        } else {
            binding.layoutSpecCost.visibility = View.GONE
        }

        if (car.description.isNotBlank()) {
            binding.tvDescription.text = car.description
            binding.tvDescription.visibility = View.VISIBLE
        }

        bindLocationPreview(car)
    }

    private fun bindLocationPreview(car: Car) {
        val shouldShow = car.effectiveStatus == Car.STATUS_LISTED &&
            (car.latitude != 0.0 || car.longitude != 0.0)

        if (!shouldShow) {
            binding.layoutLocationMap.visibility = View.GONE
            lastCarLatLng = null
            routePolyline?.remove()
            routePolyline = null
            routeJob?.cancel()
            routeJob = null
            return
        }

        val carLatLng = LatLng(car.latitude, car.longitude)
        lastCarLatLng = carLatLng
        binding.layoutLocationMap.visibility = View.VISIBLE
        binding.tvPickupAddress.text = car.locationName.ifBlank {
            getString(R.string.location_lat_lng_format, car.latitude, car.longitude)
        }
        binding.tvPickupDistance.visibility = View.GONE

        binding.btnOpenMapFull.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java).apply {
                putExtra(MapActivity.EXTRA_FOCUS_CAR_ID, car.id)
            })
        }

        renderCarLocationOnMap()
        requestUserLocationIfPossible()
    }

    private fun requestUserLocationIfPossible() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            fetchUserLocationAndRenderRoute()
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchUserLocationAndRenderRoute() {
        val carLatLng = lastCarLatLng ?: return
        try {
            LocationServices.getFusedLocationProviderClient(this)
                .lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        userLatLng = LatLng(location.latitude, location.longitude)
                        renderDistanceAndRoute(location, carLatLng)
                    }
                }
        } catch (_: SecurityException) {
            // ignore
        }
    }

    private fun renderDistanceAndRoute(userLocation: Location, carLatLng: LatLng) {
        val results = FloatArray(1)
        Location.distanceBetween(
            userLocation.latitude, userLocation.longitude,
            carLatLng.latitude, carLatLng.longitude, results
        )
        val meters = results[0].toInt()
        binding.tvPickupDistance.text = if (meters < 1000) {
            getString(R.string.distance_meters_format, meters)
        } else {
            getString(R.string.distance_km_format, meters / 1000.0)
        }
        binding.tvPickupDistance.visibility = View.VISIBLE

        routeJob?.cancel()
        routeJob = lifecycleScope.launch {
            val origin = LatLng(userLocation.latitude, userLocation.longitude)
            val map = googleMap ?: return@launch
            val polylineOptions = PolylineOptions()
                .add(origin, carLatLng)
                .width(6f)
                .color(ContextCompat.getColor(this@DetailActivity, R.color.accent))
                .geodesic(true)
            routePolyline?.remove()
            routePolyline = map.addPolyline(polylineOptions)
        }
    }

    private fun renderCarLocationOnMap() {
        val map = googleMap ?: return
        val carLatLng = lastCarLatLng ?: return
        carMarker?.remove()
        carMarker = map.addMarker(MarkerOptions().position(carLatLng))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(carLatLng, 14.5f))
    }

    private fun setupDots(count: Int) {
        binding.layoutDots.removeAllViews()
        if (count <= 1) return
        val dots = Array(count) { View(this).apply {
            val size = (6 * resources.displayMetrics.density).toInt()
            layoutParams = android.widget.LinearLayout.LayoutParams(size, size).apply {
                marginStart = (3 * resources.displayMetrics.density).toInt()
                marginEnd = (3 * resources.displayMetrics.density).toInt()
            }
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(androidx.core.content.ContextCompat.getColor(this@DetailActivity, R.color.divider))
            }
            background = bg
        }}
        dots.forEach { binding.layoutDots.addView(it) }
        updateDot(dots, 0)

        binding.vpImages.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDot(dots, position)
                if (!BuildConfig.IMAGE_PRELOAD_ALL) {
                    imageSliderAdapter.preloadAround(this@DetailActivity, centerIndex = position, radius = 1)
                }
            }
        })
    }

    private fun updateDot(dots: Array<View>, selected: Int) {
        dots.forEachIndexed { i, dot ->
            val color = if (i == selected) R.color.accent else R.color.divider
            (dot.background as? android.graphics.drawable.GradientDrawable)
                ?.setColor(androidx.core.content.ContextCompat.getColor(this, color))
        }
    }

    private fun showOwnerName(car: Car) {
        binding.tvOwnerName.text = getString(R.string.owner_format, car.ownerName)
        binding.tvOwnerName.visibility = View.VISIBLE
    }

    private fun showStatusBanner(text: String, colorRes: Int) {
        binding.tvStatusBanner.text = text
        val bg = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 8f * resources.displayMetrics.density
            setColor(androidx.core.content.ContextCompat.getColor(this@DetailActivity, colorRes))
        }
        binding.tvStatusBanner.background = bg
        binding.tvStatusBanner.visibility = View.VISIBLE
    }

    private fun showBidDialog(car: Car, balance: Int, existingBid: Bid? = null) {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogPlaceBidBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.sliderDuration.valueTo = car.maxRentalDays.toFloat().coerceAtLeast(1f)
        if (existingBid != null) {
            dialogBinding.sliderDuration.value = existingBid.rentalDays.coerceAtLeast(1).toFloat()
            dialogBinding.etDailyRate.setText(existingBid.dailyRate.toString())
            dialogBinding.etMessage.setText(existingBid.message)
            dialogBinding.tvDurationValue.text = getString(R.string.days_format, existingBid.rentalDays)
            dialogBinding.btnConfirmBid.text = getString(R.string.save_changes)
        } else {
            dialogBinding.sliderDuration.value = 1f
            dialogBinding.etDailyRate.setText(car.dailyCost.toString())
            dialogBinding.tvDurationValue.text = getString(R.string.day_format, 1)
        }

        fun updateTotal() {
            val rate = dialogBinding.etDailyRate.text?.toString()?.toIntOrNull() ?: 0
            val days = dialogBinding.sliderDuration.value.toInt()
            val total = rate * days
            dialogBinding.tvBidTotal.text = getString(R.string.credits_format, total)
            dialogBinding.tvBalance.text = getString(R.string.credits_format, balance)

            val requiredExtra = if (existingBid != null) (total - existingBid.totalAmount).coerceAtLeast(0) else total
            val remaining = balance - requiredExtra
            dialogBinding.tvAfterBid.text = getString(R.string.credits_format, remaining)
            dialogBinding.tvAfterBid.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    this,
                    if (remaining >= 0) R.color.credit_positive else R.color.error
                )
            )
            val insufficient = remaining < 0
            dialogBinding.btnConfirmBid.isEnabled = rate > 0 && !insufficient
            dialogBinding.btnConfirmBid.alpha = if (dialogBinding.btnConfirmBid.isEnabled) 1f else 0.5f
            dialogBinding.tvError.visibility = if (insufficient) View.VISIBLE else View.GONE
            dialogBinding.tvError.text = if (insufficient) getString(R.string.error_insufficient_credit, balance) else ""
        }

        dialogBinding.sliderDuration.addOnChangeListener { _, value, _ ->
            dialogBinding.tvDurationValue.text = if (value.toInt() == 1)
                getString(R.string.day_format, 1) else getString(R.string.days_format, value.toInt())
            updateTotal()
        }
        dialogBinding.etDailyRate.setOnEditorActionListener { _, _, _ -> updateTotal(); false }
        dialogBinding.etDailyRate.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { updateTotal() }
        })
        updateTotal()

        dialogBinding.btnConfirmBid.setOnClickListener {
            val rate = dialogBinding.etDailyRate.text?.toString()?.toIntOrNull() ?: 0
            val days = dialogBinding.sliderDuration.value.toInt()
            val message = dialogBinding.etMessage.text?.toString()?.trim() ?: ""
            if (existingBid != null) {
                viewModel.updateBid(existingBid, rate, days, message)
            } else {
                viewModel.placeBid(rate, days, message)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_car)
            .setMessage(R.string.confirm_delete_car)
            .setPositiveButton(R.string.delete_car) { _, _ -> setLoading(true); viewModel.deleteCar() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmUnlist() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.unlist_car)
            .setMessage(R.string.confirm_unlist_car)
            .setPositiveButton(R.string.unlist_car) { _, _ -> setLoading(true); viewModel.unlistCar() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmApproveBid(bid: Bid) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.approve_bid)
            .setMessage(getString(R.string.confirm_approve_bid, bid.bidderName, bid.totalAmount))
            .setPositiveButton(R.string.approve) { _, _ -> viewModel.approveBid(bid) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmRejectBid(bid: Bid) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reject)
            .setMessage(getString(R.string.confirm_reject_bid, bid.bidderName, bid.totalAmount))
            .setPositiveButton(R.string.reject) { _, _ -> viewModel.rejectBid(bid) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmMarkComplete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.mark_complete)
            .setMessage(R.string.confirm_mark_complete)
            .setPositiveButton(R.string.mark_complete) { _, _ -> viewModel.markComplete() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openChat(otherUserId: String, otherUserName: String) {
        lifecycleScope.launch {
            val convoId = chatViewModel.getOrCreateConversation(otherUserId, otherUserName)
            startActivity(Intent(this@DetailActivity, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_CONVERSATION_ID, convoId)
                putExtra(ChatActivity.EXTRA_TITLE, otherUserName)
            })
        }
    }

    private fun openEditCar(carId: String) {
        startActivity(Intent(this, com.example.assignment3_cos30017.ui.addcar.AddCarActivity::class.java).apply {
            putExtra(com.example.assignment3_cos30017.ui.addcar.AddCarActivity.EXTRA_CAR_ID, carId)
        })
    }

    private fun openEditListing(carId: String) {
        startActivity(Intent(this, com.example.assignment3_cos30017.ui.listcar.ListCarActivity::class.java).apply {
            putExtra(com.example.assignment3_cos30017.ui.listcar.ListCarActivity.EXTRA_CAR_ID, carId)
            putExtra(com.example.assignment3_cos30017.ui.listcar.ListCarActivity.EXTRA_EDIT_MODE, true)
        })
    }

    private fun snack(resId: Int) {
        Snackbar.make(binding.root, resId, Snackbar.LENGTH_SHORT).show()
    }
}
