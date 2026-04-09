package com.example.assignment3_cos30017.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.databinding.ItemCarGridBinding

class CarGridAdapter(
    private val onCarClick: (Car) -> Unit,
    private val onFavouriteClick: (Car) -> Unit,
    private val onCarLongPress: ((Car) -> Unit)? = null,
    private val showPrice: Boolean = true,
    private val garageMode: Boolean = false,
    private val showFavourite: Boolean = true,
    private val currentUserId: String? = null
) : ListAdapter<Car, CarGridAdapter.CarViewHolder>(CarDiffCallback()) {

    private var favouriteIds: Set<String> = emptySet()
    private var statusOverrides: Map<String, String> = emptyMap()
    private var statusBadgeDrawables: Map<String, Int> = emptyMap()

    fun updateFavourites(ids: Set<String>) {
        if (favouriteIds == ids) return
        val old = favouriteIds
        favouriteIds = ids
        val list = currentList
        for (i in list.indices) {
            val id = list[i].id
            if ((id in old) != (id in ids)) {
                notifyItemChanged(i, PAYLOAD_FAVOURITE)
            }
        }
    }

    fun updateStatusOverrides(overrides: Map<String, String>, drawables: Map<String, Int> = emptyMap()) {
        if (statusOverrides == overrides && statusBadgeDrawables == drawables) return
        statusOverrides = overrides
        statusBadgeDrawables = drawables
        notifyDataSetChanged()
    }

    inner class CarViewHolder(
        private val binding: ItemCarGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(car: Car) {
            if (car.imageUrls.isNotEmpty()) {
                Glide.with(binding.root.context).load(car.imageUrls.first()).into(binding.ivCar)
            } else {
                binding.ivCar.setImageResource(R.drawable.ic_car_logo)
            }

            binding.tvCarName.text = car.title

            if (garageMode) {
                binding.tvModel.text = binding.root.context.getString(
                    R.string.car_model_year_format,
                    car.model,
                    car.year
                )
            } else {
                binding.tvModel.text = car.model
            }

            val overrideText = statusOverrides[car.id]
            if (overrideText != null) {
                val drawableRes = statusBadgeDrawables[car.id] ?: R.drawable.bg_rented_badge
                binding.tvRentedBadge.text = overrideText
                binding.tvRentedBadge.setBackgroundResource(drawableRes)
                binding.tvRentedBadge.visibility = android.view.View.VISIBLE
                binding.ivCar.alpha = 1.0f
            } else when (car.effectiveStatus) {
                Car.STATUS_UNLISTED -> {
                    binding.tvRentedBadge.text = binding.root.context.getString(R.string.status_unlisted)
                    binding.tvRentedBadge.setBackgroundResource(R.drawable.bg_rented_badge)
                    binding.tvRentedBadge.visibility = android.view.View.VISIBLE
                    binding.ivCar.alpha = 0.5f
                }
                Car.STATUS_LISTED -> {
                    if (!garageMode && currentUserId != null && car.ownerId == currentUserId) {
                        binding.tvRentedBadge.text = binding.root.context.getString(R.string.my_cars)
                        binding.tvRentedBadge.setBackgroundResource(R.drawable.bg_rented_badge)
                        binding.tvRentedBadge.visibility = android.view.View.VISIBLE
                    } else if (garageMode) {
                        binding.tvRentedBadge.text = binding.root.context.getString(R.string.status_listed)
                        binding.tvRentedBadge.setBackgroundResource(R.drawable.bg_badge_green)
                        binding.tvRentedBadge.visibility = android.view.View.VISIBLE
                    } else {
                        binding.tvRentedBadge.visibility = android.view.View.GONE
                    }
                    binding.ivCar.alpha = 1.0f
                }
                Car.STATUS_RENTED -> {
                    binding.tvRentedBadge.text = binding.root.context.getString(R.string.tab_rented)
                    binding.tvRentedBadge.setBackgroundResource(R.drawable.bg_rented_badge)
                    binding.tvRentedBadge.visibility = android.view.View.VISIBLE
                    binding.ivCar.alpha = 0.5f
                }
                else -> {
                    binding.tvRentedBadge.visibility = android.view.View.GONE
                    binding.ivCar.alpha = 1.0f
                }
            }

            if (showPrice && car.dailyCost > 0) {
                val context = binding.root.context
                binding.tvCost.text = context.getString(R.string.credits_per_day_format, car.dailyCost)
                binding.tvCost.visibility = android.view.View.VISIBLE
            } else {
                binding.tvCost.visibility = android.view.View.GONE
            }

            bindFavouriteUi(car)

            binding.root.setOnClickListener { onCarClick(car) }
            if (onCarLongPress != null) {
                binding.root.setOnLongClickListener {
                    onCarLongPress.invoke(car)
                    true
                }
            } else {
                binding.root.setOnLongClickListener(null)
                binding.root.isLongClickable = false
            }
        }

        fun bindFavouriteOnly(car: Car) {
            bindFavouriteUi(car)
        }

        private fun bindFavouriteUi(car: Car) {
            if (showFavourite) {
                binding.btnFavourite.visibility = android.view.View.VISIBLE
                val isFav = car.id in favouriteIds
                binding.btnFavourite.setImageResource(
                    if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                )
                binding.btnFavourite.setOnClickListener { onFavouriteClick(car) }
            } else {
                binding.btnFavourite.visibility = android.view.View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.all { it === PAYLOAD_FAVOURITE }) {
            holder.bindFavouriteOnly(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private class CarDiffCallback : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(oldItem: Car, newItem: Car) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Car, newItem: Car) = oldItem == newItem
    }

    companion object {
        private val PAYLOAD_FAVOURITE = Any()
    }
}
