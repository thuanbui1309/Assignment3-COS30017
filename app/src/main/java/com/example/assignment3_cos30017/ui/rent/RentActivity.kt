package com.example.assignment3_cos30017.ui.rent

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.databinding.ActivityRentBinding
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.viewmodel.RentViewModel
import com.google.android.material.snackbar.Snackbar

class RentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CAR_ID = "extra_car_id"
    }

    private lateinit var binding: ActivityRentBinding
    private val viewModel: RentViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val carId = intent.getStringExtra(EXTRA_CAR_ID) ?: run { finish(); return }
        viewModel.loadCar(carId)

        binding.toolbar.setNavigationOnClickListener { finish() }
        setupSlider()
        setupButtons()
        observeViewModel()
    }

    private fun setupSlider() {
        binding.sliderDuration.addOnChangeListener { _, value, _ ->
            val days = value.toInt()
            viewModel.setRentalDays(days)
            binding.tvDurationValue.text = if (days == 1) getString(R.string.day_format, days)
            else getString(R.string.days_format, days)
        }
        binding.tvDurationValue.text = getString(R.string.day_format, 1)
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            if (!validateForm()) return@setOnClickListener
            viewModel.confirmBooking(
                binding.etName.text?.toString()?.trim() ?: "",
                binding.etPhone.text?.toString()?.trim() ?: "",
                binding.etEmail.text?.toString()?.trim() ?: ""
            )
        }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val name = binding.etName.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) { binding.tilName.error = getString(R.string.error_field_required); valid = false }
        else binding.tilName.error = null

        val phone = binding.etPhone.text?.toString()?.trim() ?: ""
        if (phone.isEmpty()) { binding.tilPhone.error = getString(R.string.error_field_required); valid = false }
        else binding.tilPhone.error = null

        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        if (email.isEmpty()) { binding.tilEmail.error = getString(R.string.error_field_required); valid = false }
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.invalid_email); valid = false
        } else binding.tilEmail.error = null

        return valid
    }

    private fun observeViewModel() {
        viewModel.selectedCar.observe(this) { car ->
            binding.tvRentCarName.text = car.title
            binding.tvRentCarCost.text = getString(R.string.credits_per_day_format, car.dailyCost)
            if (car.imageUrls.isNotEmpty()) {
                com.bumptech.glide.Glide.with(this).load(car.imageUrls.first()).into(binding.ivRentCar)
            }
        }

        viewModel.totalCost.observe(this) { cost ->
            binding.tvTotalCost.text = getString(R.string.credits_format, cost)
            binding.tvDailyCost.text = getString(R.string.credits_format, viewModel.selectedCar.value?.dailyCost ?: 0)
            val days = viewModel.rentalDays.value ?: 1
            binding.tvSummaryDuration.text = if (days == 1) getString(R.string.day_format, days) else getString(R.string.days_format, days)
            val balance = viewModel.creditBalance.value ?: 0
            binding.tvAfterBooking.text = getString(R.string.credits_format, balance - cost)
        }

        viewModel.creditBalance.observe(this) { balance ->
            binding.tvBalance.text = getString(R.string.credits_format, balance)
        }

        viewModel.validationError.observe(this) { error ->
            binding.tvValidationError.visibility = if (error != null) View.VISIBLE else View.GONE
            binding.tvValidationError.text = error
            binding.btnSave.isEnabled = error == null
            binding.btnSave.alpha = if (error == null) 1.0f else 0.5f
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !loading && viewModel.validationError.value == null
        }

        viewModel.bookingSuccess.observe(this) { success ->
            if (success == true) {
                Snackbar.make(binding.root, R.string.booking_confirmed, Snackbar.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else if (success == false) {
                Snackbar.make(binding.root, R.string.error_insufficient_credit, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
