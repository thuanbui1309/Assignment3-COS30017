package com.example.assignment3_cos30017.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.data.repository.AuthRepository
import com.example.assignment3_cos30017.data.repository.CarRepository
import com.example.assignment3_cos30017.databinding.ActivityMainBinding
import com.example.assignment3_cos30017.ui.adapter.CarGridAdapter
import com.example.assignment3_cos30017.ui.auth.LoginActivity
import com.example.assignment3_cos30017.ui.cars.CarsActivity
import com.example.assignment3_cos30017.ui.chat.ChatListActivity
import com.example.assignment3_cos30017.ui.detail.DetailActivity
import com.example.assignment3_cos30017.ui.me.MeActivity
import com.example.assignment3_cos30017.ui.map.MapActivity
import com.example.assignment3_cos30017.ui.notifications.NotificationsActivity
import com.example.assignment3_cos30017.util.BottomNavBadgeHelper
import com.example.assignment3_cos30017.util.DialogHelper
import com.example.assignment3_cos30017.util.FcmTokenHelper
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.util.NavigationHelper
import com.example.assignment3_cos30017.util.ToolbarBadgeHelper

import com.example.assignment3_cos30017.viewmodel.CarViewModel
import com.example.assignment3_cos30017.viewmodel.ChatBadgeViewModel
import com.example.assignment3_cos30017.viewmodel.NotificationsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CarViewModel by viewModels()
    private val notificationsViewModel: NotificationsViewModel by viewModels()
    private val chatBadgeViewModel: ChatBadgeViewModel by viewModels()
    private lateinit var carGridAdapter: CarGridAdapter
    private val authRepository = AuthRepository()
    private var currentLanguage: String = ""
    private var currentFilterMode: CarViewModel.FilterMode = CarViewModel.FilterMode.ALL
    private var chatBadgeViews: ToolbarBadgeHelper.BadgeViews? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* optional */ }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!authRepository.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSearchBar()
        setupSortButton()
        setupCarGrid()
        setupFilterChips()
        setupBottomNav()
        observeViewModel()
        observeBadges()

        requestNotificationPermissionIfNeeded()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            lifecycleScope.launch { FcmTokenHelper.syncTokenForUser(this@MainActivity, uid) }
        }

        currentLanguage = LocaleHelper.getLanguage(this)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_home
        val savedLanguage = LocaleHelper.getLanguage(this)
        if (currentLanguage.isNotEmpty() && currentLanguage != savedLanguage) {
            currentLanguage = savedLanguage
            recreate()
        }
    }

    private fun setupToolbar() {
        NavigationHelper.setup(this, binding.toolbar)

        chatBadgeViews = ToolbarBadgeHelper.bindActionIconWithBadge(
            activity = this,
            toolbar = binding.toolbar,
            menuItemId = R.id.action_chat,
            iconRes = R.drawable.ic_chat,
            onClick = { startActivity(Intent(this, ChatListActivity::class.java)) }
        )
    }

    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { viewModel.searchCars(s?.toString() ?: "") }
        })
    }

    private fun setupSortButton() {
        binding.btnSort.setOnClickListener {
            val bottomSheet = BottomSheetDialog(this)
            bottomSheet.setContentView(R.layout.dialog_sort)
            bottomSheet.findViewById<TextView>(R.id.btn_sort_year)?.setOnClickListener {
                viewModel.sortCars(CarRepository.SortMode.YEAR_DESC); bottomSheet.dismiss()
            }
            bottomSheet.findViewById<TextView>(R.id.btn_sort_cost)?.setOnClickListener {
                viewModel.sortCars(CarRepository.SortMode.COST_ASC); bottomSheet.dismiss()
            }
            bottomSheet.show()
        }
    }

    private fun setupCarGrid() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        carGridAdapter = CarGridAdapter(
            onCarClick = { car -> launchDetailActivity(car) },
            onFavouriteClick = { car -> viewModel.toggleFavourite(car.id) },
            currentUserId = uid
        )
        val spanCount = (resources.configuration.screenWidthDp / 180).coerceIn(2, 4)
        binding.rvCars.apply {
            layoutManager = GridLayoutManager(this@MainActivity, spanCount)
            itemAnimator = null
            adapter = carGridAdapter
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) setFilter(CarViewModel.FilterMode.ALL)
        }

        binding.chipMyCars.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) setFilter(CarViewModel.FilterMode.MY_CARS)
        }

        binding.chipFavourites.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) setFilter(CarViewModel.FilterMode.FAVOURITES)
        }

        binding.chipLocation.setOnClickListener {
            MapActivity.startNearMe(this)
        }
    }

    private fun setFilter(mode: CarViewModel.FilterMode) {
        currentFilterMode = mode
        viewModel.setFilterMode(mode)
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_cars -> {
                    startActivity(Intent(this, CarsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_me -> {
                    startActivity(Intent(this, MeActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun launchDetailActivity(car: Car) {
        startActivity(Intent(this, DetailActivity::class.java).apply {
            putExtra(DetailActivity.EXTRA_CAR_ID, car.id)
            putExtra(DetailActivity.EXTRA_ENTRY_CONTEXT, com.example.assignment3_cos30017.viewmodel.DetailViewModel.CONTEXT_MARKETPLACE)
        })
    }

    private fun observeViewModel() {
        viewModel.displayedCars.observe(this) { cars ->
            carGridAdapter.submitList(cars)
            binding.rvCars.visibility = if (cars.isNotEmpty()) View.VISIBLE else View.GONE
            if (cars.isEmpty()) {
                binding.tvNoCars.text = when (currentFilterMode) {
                    CarViewModel.FilterMode.MY_CARS -> getString(R.string.empty_my_cars_home)
                    CarViewModel.FilterMode.FAVOURITES -> getString(R.string.empty_favourites_home)
                    CarViewModel.FilterMode.ALL -> getString(R.string.no_cars)
                }
                binding.tvNoCars.visibility = View.VISIBLE
            } else {
                binding.tvNoCars.visibility = View.GONE
            }
            // Hide the section title on Home (user requested).
            binding.tvSectionTitle.visibility = android.view.View.GONE
        }

        viewModel.creditBalance.observe(this) { balance ->
            binding.tvCreditBalance.text = getString(R.string.credits_format, balance)
        }

        viewModel.favouriteIds.observe(this) { ids ->
            carGridAdapter.updateFavourites(ids)
        }
    }

    private fun observeBadges() {
        BottomNavBadgeHelper.observeNotificationUnread(
            this,
            binding.bottomNav,
            notificationsViewModel.unreadCount
        )
        chatBadgeViewModel.unreadConversations.observe(this) { count ->
            ToolbarBadgeHelper.renderCount(chatBadgeViews, count)
        }
    }
}
