package com.example.assignment3_cos30017.ui.addcar

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.data.repository.CarRepository
import com.example.assignment3_cos30017.databinding.ActivityAddCarBinding
import com.example.assignment3_cos30017.ui.adapter.PhotoAdapter
import com.example.assignment3_cos30017.util.LocaleHelper
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AddCarActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CAR_ID = "extra_car_id"
    }

    private lateinit var binding: ActivityAddCarBinding
    private val carRepository = CarRepository()
    private val newPhotoUris = mutableListOf<Uri>()
    private val existingImageUrls = mutableListOf<String>()
    private lateinit var photoAdapter: PhotoAdapter

    private var editCarId: String? = null
    private var editCar: Car? = null

    private val isEditMode get() = editCarId != null

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        newPhotoUris.addAll(uris)
        refreshPhotos()
        if (totalPhotoCount() > 0) binding.tvPhotoError.visibility = View.GONE
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editCarId = intent.getStringExtra(EXTRA_CAR_ID)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = getString(if (isEditMode) R.string.edit_car else R.string.add_car_title)
        binding.btnSubmit.text = getString(if (isEditMode) R.string.save_changes else R.string.add_car)

        val categories = arrayOf("Supercar", "Hypercar", "Grand Tourer", "Sedan", "Electric", "SUV", "Hatchback")
        binding.etModel.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories))

        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val years = (currentYear downTo 1990).map { it.toString() }.toTypedArray()
        binding.etYear.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, years))
        binding.etYear.setOnClickListener { binding.etYear.showDropDown() }

        photoAdapter = PhotoAdapter(
            onRemove = { position ->
                if (position < existingImageUrls.size) {
                    existingImageUrls.removeAt(position)
                } else {
                    newPhotoUris.removeAt(position - existingImageUrls.size)
                }
                refreshPhotos()
            },
            onClick = { position -> showFullPhoto(position) }
        )
        binding.rvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPhotos.adapter = photoAdapter

        binding.btnAddPhoto.setOnClickListener { photoPicker.launch("image/*") }
        binding.btnSubmit.setOnClickListener { submitCar() }

        if (isEditMode) {
            setLoading(true)
            loadExistingCar()
        }
    }

    private fun loadExistingCar() {
        val carId = editCarId ?: return
        lifecycleScope.launch {
            val car = carRepository.getCarById(carId).firstOrNull() ?: run {
                setLoading(false); return@launch
            }
            editCar = car
            binding.etTitle.setText(car.title)
            binding.etModel.setText(car.model, false)
            binding.etYear.setText(car.year.toString(), false)
            binding.etDescription.setText(car.description)

            existingImageUrls.addAll(car.imageUrls)
            refreshPhotos()
            setLoading(false)
        }
    }

    private fun totalPhotoCount() = existingImageUrls.size + newPhotoUris.size

    private fun refreshPhotos() {
        photoAdapter.setMixed(existingImageUrls, newPhotoUris)
        binding.btnAddPhoto.text = getString(R.string.photos_count, totalPhotoCount())
        binding.rvPhotos.visibility = if (totalPhotoCount() > 0) View.VISIBLE else View.GONE
    }

    private fun showFullPhoto(position: Int) {
        val item = photoAdapter.getItem(position)
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
            setOnClickListener { dialog.dismiss() }
        }
        val source: Any = when (item) {
            is PhotoAdapter.PhotoItem.Local -> item.uri
            is PhotoAdapter.PhotoItem.Remote -> item.url
        }
        Glide.with(this).load(source).into(imageView)
        dialog.setContentView(imageView)
        dialog.show()
    }

    private fun submitCar() {
        if (!binding.btnSubmit.isEnabled) return
        binding.btnSubmit.isEnabled = false

        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        val model = binding.etModel.text?.toString()?.trim() ?: ""
        val yearStr = binding.etYear.text?.toString()?.trim() ?: ""
        val year = yearStr.toIntOrNull() ?: 0
        val description = binding.etDescription.text?.toString()?.trim() ?: ""

        var valid = true
        if (title.isEmpty()) { binding.tilTitle.error = getString(R.string.error_field_required); valid = false }
        else binding.tilTitle.error = null
        if (model.isEmpty()) { binding.tilModel.error = getString(R.string.error_field_required); valid = false }
        else binding.tilModel.error = null
        if (yearStr.isEmpty() || year == 0) {
            binding.tilYear.error = getString(R.string.error_field_required); valid = false
        } else binding.tilYear.error = null
        if (description.isEmpty()) { binding.tilDescription.error = getString(R.string.error_field_required); valid = false }
        else binding.tilDescription.error = null
        if (totalPhotoCount() == 0) {
            binding.tvPhotoError.text = getString(R.string.error_photo_required)
            binding.tvPhotoError.visibility = View.VISIBLE
            valid = false
        } else {
            binding.tvPhotoError.visibility = View.GONE
        }
        if (!valid) { binding.btnSubmit.isEnabled = true; return }

        setLoading(true)
        if (isEditMode) updateExistingCar(title, model, year, description)
        else createNewCar(title, model, year, description)
    }

    private fun createNewCar(title: String, model: String, year: Int, description: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val car = Car(
            ownerId = user.uid,
            ownerName = user.displayName ?: getString(R.string.generic_user),
            title = title, model = model, year = year, description = description
        )
        lifecycleScope.launch {
            try {
                carRepository.addCar(car, newPhotoUris)
                Snackbar.make(binding.root, R.string.car_added_success, Snackbar.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Snackbar.make(binding.root, e.localizedMessage ?: getString(R.string.generic_error), Snackbar.LENGTH_SHORT).show()
                setLoading(false)
            }
        }
    }

    private fun updateExistingCar(title: String, model: String, year: Int, description: String) {
        val carId = editCarId ?: return
        val original = editCar ?: return
        lifecycleScope.launch {
            try {
                val uploadedNewUrls = newPhotoUris.map { uri ->
                    carRepository.uploadImagePublic(uri)
                }
                val allUrls = existingImageUrls + uploadedNewUrls

                val updated = original.copy(
                    title = title, model = model, year = year, description = description,
                    imageUrls = allUrls
                )
                carRepository.updateCar(updated)
                Snackbar.make(binding.root, R.string.car_updated_success, Snackbar.LENGTH_SHORT).show()
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
