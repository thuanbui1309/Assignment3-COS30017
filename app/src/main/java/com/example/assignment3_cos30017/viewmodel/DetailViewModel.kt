package com.example.assignment3_cos30017.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3_cos30017.data.model.Bid
import com.example.assignment3_cos30017.data.model.Booking
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.data.model.AppNotification
import com.example.assignment3_cos30017.data.repository.BidRepository
import com.example.assignment3_cos30017.data.repository.BookingRepository
import com.example.assignment3_cos30017.data.repository.CarRepository
import com.example.assignment3_cos30017.data.repository.NotificationRepository
import com.example.assignment3_cos30017.data.repository.WalletRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class DetailViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val carRepository = CarRepository()
    private val bookingRepository = BookingRepository()
    private val bidRepository = BidRepository()
    private val walletRepository = WalletRepository()
    private val notificationRepository = NotificationRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _uiState = MutableLiveData<DetailUiState>(DetailUiState.Loading)
    val uiState: LiveData<DetailUiState> = _uiState

    private val _actionResult = MutableLiveData<ActionResult?>()
    val actionResult: LiveData<ActionResult?> = _actionResult

    private var loadedCar: Car? = null
    private var loadedBooking: Booking? = null

    val entryContext: String get() = savedStateHandle[EXTRA_ENTRY_CONTEXT] ?: CONTEXT_MARKETPLACE
    private val carId: String get() = savedStateHandle[EXTRA_CAR_ID] ?: ""
    private val bookingId: String? get() = savedStateHandle[EXTRA_BOOKING_ID]

    fun load() {
        if (carId.isBlank()) return
        when (entryContext) {
            CONTEXT_MY_RENTALS -> loadForRentals()
            else -> loadForGeneral()
        }
    }

    private fun loadForRentals() {
        val bid = bookingId
        if (bid == null) return
        viewModelScope.launch {
            bookingRepository.getBookingById(bid).flatMapLatest { booking ->
                if (booking == null) return@flatMapLatest flowOf<DetailUiState>(DetailUiState.Loading)
                loadedBooking = booking
                carRepository.getCarById(booking.carId).flatMapLatest { car ->
                    if (car == null) return@flatMapLatest flowOf<DetailUiState>(DetailUiState.Loading)
                    loadedCar = car
                    val state = when (booking.status) {
                        Booking.STATUS_ACTIVE -> DetailUiState.RenterActiveBooking(car, booking)
                        else -> DetailUiState.RenterBookingHistory(car, booking)
                    }
                    flowOf(state)
                }
            }.collectLatest { _uiState.postValue(it) }
        }
    }

    private fun loadForGeneral() {
        viewModelScope.launch {
            val carFlow = carRepository.getCarById(carId)
            val bidsFlow = bidRepository.getBidsForCar(carId)
            val balanceFlow = if (currentUserId.isNotEmpty())
                walletRepository.getBalance(currentUserId) else flowOf(0)

            combine(carFlow, bidsFlow, balanceFlow) { car, bids, balance ->
                if (car == null) return@combine DetailUiState.Loading
                loadedCar = car
                val isOwner = car.ownerId == currentUserId

                // Filter bids and myBid by current listing
                val currentListingBids = filterBidsByListing(car, bids)
                val myBid = if (currentUserId.isNotEmpty()) {
                    currentListingBids.firstOrNull {
                        it.bidderId == currentUserId && it.status == Bid.STATUS_PENDING
                    }
                } else null

                resolveState(car, isOwner, currentListingBids, myBid, balance)
            }.collectLatest { _uiState.postValue(it) }
        }
    }

    private fun filterBidsByListing(car: Car, bids: List<Bid>): List<Bid> = when {
        car.listingId.isNotBlank() -> bids.filter { it.listingId == car.listingId }
        car.listedAt > 0L -> bids.filter { it.createdAt >= car.listedAt }
        else -> bids
    }

    private suspend fun resolveState(
        car: Car, isOwner: Boolean, currentListingBids: List<Bid>, myBid: Bid?, balance: Int
    ): DetailUiState {
        val status = car.effectiveStatus
        return when {
            isOwner && status == Car.STATUS_UNLISTED -> DetailUiState.OwnerGarage(car)

            isOwner && status == Car.STATUS_LISTED && entryContext == CONTEXT_MARKETPLACE ->
                DetailUiState.OwnerMarketplace(car, currentListingBids)

            isOwner && status == Car.STATUS_LISTED ->
                DetailUiState.OwnerListed(car, currentListingBids)

            isOwner && status == Car.STATUS_RENTED -> {
                val booking = bookingRepository.getActiveBookingForCar(car.id)
                loadedBooking = booking
                if (booking != null) DetailUiState.OwnerRented(car, booking)
                else DetailUiState.OwnerListed(car, currentListingBids)
            }

            !isOwner && status == Car.STATUS_RENTED ->
                DetailUiState.MarketplaceRented(car)

            !isOwner && status == Car.STATUS_LISTED && myBid != null ->
                DetailUiState.RenterAlreadyBid(car, balance, myBid, currentListingBids)

            !isOwner && status == Car.STATUS_LISTED ->
                DetailUiState.RenterBrowsing(car, balance, currentListingBids)

            else -> DetailUiState.Loading
        }
    }

    // --- Owner Actions ---


    fun unlistCar() {
        val car = loadedCar ?: return
        viewModelScope.launch {
            try {
                val pendingBids = bidRepository.getPendingBidsForCar(car.id)
                pendingBids.forEach { bid ->
                    bidRepository.rejectBid(bid.id)
                    walletRepository.refundBid(bid.bidderId, bid.totalAmount, "Refund: ${car.title} unlisted")
                    notificationRepository.create(
                        AppNotification(
                            toUserId = bid.bidderId,
                            type = AppNotification.TYPE_BIDS_CANCELLED_BY_OWNER,
                            carId = car.id,
                            carTitle = car.title,
                            actorUserId = car.ownerId,
                            actorName = car.ownerName,
                            bidId = bid.id
                        )
                    )
                }
                carRepository.clearRentalTerms(car.id)
                _actionResult.postValue(ActionResult.CarUnlisted)
            } catch (e: Exception) {
                _actionResult.postValue(
                    e.localizedMessage?.takeIf { it.isNotBlank() }?.let { ActionResult.Error(it) }
                        ?: ActionResult.ErrorRes(com.example.assignment3_cos30017.R.string.generic_error)
                )
            }
        }
    }

    fun deleteCar() {
        val car = loadedCar ?: return
        viewModelScope.launch {
            try {
                carRepository.deleteCar(car.id)
                _actionResult.postValue(ActionResult.CarDeleted)
            } catch (e: Exception) {
                _actionResult.postValue(
                    e.localizedMessage?.takeIf { it.isNotBlank() }?.let { ActionResult.Error(it) }
                        ?: ActionResult.ErrorRes(com.example.assignment3_cos30017.R.string.generic_error)
                )
            }
        }
    }

    fun markComplete() {
        val car = loadedCar ?: return
        val booking = loadedBooking ?: return
        viewModelScope.launch {
            try {
                bookingRepository.completeBooking(booking.id)
                carRepository.clearRentalTerms(car.id)
                _actionResult.postValue(ActionResult.BookingCompleted)
            } catch (e: Exception) {
                _actionResult.postValue(
                    e.localizedMessage?.takeIf { it.isNotBlank() }?.let { ActionResult.Error(it) }
                        ?: ActionResult.ErrorRes(com.example.assignment3_cos30017.R.string.generic_error)
                )
            }
        }
    }

    fun approveBid(bid: Bid) {
        val car = loadedCar ?: return
        viewModelScope.launch {
            try {
                val pendingBefore = bidRepository.getPendingBidsForCar(car.id)
                val losers = pendingBefore.filter { it.id != bid.id }

                bidRepository.approveBid(bid.id)
                bidRepository.rejectAllPendingBids(car.id, bid.id)

                losers.forEach { loser ->
                    walletRepository.refundBid(loser.bidderId, loser.totalAmount, "Bid refund: ${car.title}")
                    notificationRepository.create(
                        AppNotification(
                            toUserId = loser.bidderId,
                            type = AppNotification.TYPE_BID_APPROVED_LOSER,
                            carId = car.id,
                            carTitle = car.title,
                            actorUserId = bid.bidderId,
                            actorName = bid.bidderName,
                            bidId = loser.id
                        )
                    )
                }

                walletRepository.creditOwner(car.ownerId, bid.totalAmount, "Rental income: ${car.title} (${bid.rentalDays} days)")

                notificationRepository.create(
                    AppNotification(
                        toUserId = bid.bidderId,
                        type = AppNotification.TYPE_BID_APPROVED_WINNER,
                        carId = car.id,
                        carTitle = car.title,
                        actorUserId = car.ownerId,
                        actorName = car.ownerName,
                        bidId = bid.id,
                        amount = bid.totalAmount,
                        rentalDays = bid.rentalDays
                    )
                )

                // Notify owner as well (so the lister sees the outcome in Notifications).
                notificationRepository.create(
                    AppNotification(
                        toUserId = car.ownerId,
                        type = AppNotification.TYPE_BID_APPROVED_OWNER,
                        carId = car.id,
                        carTitle = car.title,
                        actorUserId = bid.bidderId,
                        actorName = bid.bidderName,
                        bidId = bid.id,
                        amount = bid.totalAmount,
                        rentalDays = bid.rentalDays
                    )
                )

                val booking = Booking(
                    carId = car.id, carTitle = car.title,
                    renterId = bid.bidderId, ownerId = car.ownerId,
                    renterName = bid.bidderName,
                    rentalDays = bid.rentalDays, dailyCost = bid.dailyRate,
                    totalCost = bid.totalAmount
                )
                bookingRepository.createBooking(booking)
                carRepository.setStatus(car.id, Car.STATUS_RENTED)

                _actionResult.postValue(ActionResult.BidApproved)
            } catch (e: Exception) {
                _actionResult.postValue(
                    e.localizedMessage?.takeIf { it.isNotBlank() }?.let { ActionResult.Error(it) }
                        ?: ActionResult.ErrorRes(com.example.assignment3_cos30017.R.string.generic_error)
                )
            }
        }
    }

    fun rejectBid(bid: Bid) {
        val car = loadedCar ?: return
        viewModelScope.launch {
            try {
                bidRepository.rejectBid(bid.id)
                walletRepository.refundBid(bid.bidderId, bid.totalAmount, "Bid rejected: ${car.title}")
                notificationRepository.create(
                    AppNotification(
                        toUserId = bid.bidderId,
                        type = AppNotification.TYPE_BID_REJECTED_BY_OWNER,
                        carId = car.id,
                        carTitle = car.title,
                        actorUserId = car.ownerId,
                        actorName = car.ownerName,
                        bidId = bid.id,
                        amount = bid.totalAmount,
                        rentalDays = bid.rentalDays
                    )
                )
                _actionResult.postValue(ActionResult.BidRejected)
            } catch (e: Exception) {
                _actionResult.postValue(
                    e.localizedMessage?.takeIf { it.isNotBlank() }?.let { ActionResult.Error(it) }
                        ?: ActionResult.ErrorRes(com.example.assignment3_cos30017.R.string.generic_error)
                )
            }
        }
    }

    // --- Renter Actions ---

    fun placeBid(dailyRate: Int, rentalDays: Int, message: String) {
        val car = loadedCar ?: return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val total = dailyRate * rentalDays
        viewModelScope.launch {
            try {
                walletRepository.holdBid(user.uid, total, "Bid on ${car.title} ($rentalDays days)")
                val bid = Bid(
                    carId = car.id, carTitle = car.title,
                    bidderId = user.uid, bidderName = user.displayName ?: "User",
                    ownerId = car.ownerId,
                    listingId = car.listingId,
                    dailyRate = dailyRate, rentalDays = rentalDays,
                    totalAmount = total, message = message
                )
                val bidId = bidRepository.placeBid(bid)
                notificationRepository.create(
                    AppNotification(
                        toUserId = car.ownerId,
                        type = AppNotification.TYPE_NEW_BID,
                        carId = car.id,
                        carTitle = car.title,
                        actorUserId = user.uid,
                        actorName = bid.bidderName,
                        bidId = bidId,
                        amount = total,
                        rentalDays = rentalDays
                    )
                )
                _actionResult.postValue(ActionResult.BidPlaced)
            } catch (e: Exception) {
                _actionResult.postValue(
                    e.localizedMessage?.takeIf { it.isNotBlank() }?.let { ActionResult.Error(it) }
                        ?: ActionResult.ErrorRes(com.example.assignment3_cos30017.R.string.generic_error)
                )
            }
        }
    }

    fun updateBid(existing: Bid, newDailyRate: Int, newRentalDays: Int, newMessage: String) {
        val car = loadedCar ?: return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (existing.status != Bid.STATUS_PENDING || existing.bidderId != user.uid) {
            _actionResult.postValue(ActionResult.ErrorRes(com.example.assignment3_cos30017.R.string.generic_error))
            return
        }
        val newTotal = newDailyRate * newRentalDays
        val delta = newTotal - existing.totalAmount

        viewModelScope.launch {
            try {
                if (delta > 0) {
                    walletRepository.holdBid(user.uid, delta, "Bid increase: ${car.title} ($newRentalDays days)")
                } else if (delta < 0) {
                    walletRepository.refundBid(user.uid, -delta, "Bid decrease: ${car.title}")
                }
                bidRepository.updatePendingBid(
                    bidId = existing.id,
                    dailyRate = newDailyRate,
                    rentalDays = newRentalDays,
                    totalAmount = newTotal,
                    message = newMessage
                )
                _actionResult.postValue(ActionResult.BidUpdated)
            } catch (e: Exception) {
                _actionResult.postValue(
                    e.localizedMessage?.takeIf { it.isNotBlank() }?.let { ActionResult.Error(it) }
                        ?: ActionResult.ErrorRes(com.example.assignment3_cos30017.R.string.generic_error)
                )
            }
        }
    }

    fun cancelBid(bid: Bid) {
        // Business rule: individual bid cancellation is disabled.
        // A listing can only be cancelled by unlisting the car, which cancels/refunds all bids.
        _actionResult.postValue(ActionResult.ErrorRes(com.example.assignment3_cos30017.R.string.error_bids_cancel_by_listing_only))
    }

    fun cancelBooking() {
        val car = loadedCar ?: return
        val booking = loadedBooking ?: return
        viewModelScope.launch {
            try {
                bookingRepository.cancelBooking(booking.id)
                carRepository.setStatus(car.id, Car.STATUS_LISTED)
                walletRepository.refund(booking.renterId, booking.totalCost, "Refund: ${car.title} (${booking.rentalDays} days)")
                _actionResult.postValue(ActionResult.BookingCancelled)
            } catch (e: Exception) {
                _actionResult.postValue(
                    e.localizedMessage?.takeIf { it.isNotBlank() }?.let { ActionResult.Error(it) }
                        ?: ActionResult.ErrorRes(com.example.assignment3_cos30017.R.string.generic_error)
                )
            }
        }
    }

    fun clearActionResult() { _actionResult.value = null }

    sealed class ActionResult {
        object CarListed : ActionResult()
        object CarUnlisted : ActionResult()
        object CarDeleted : ActionResult()
        object BookingCompleted : ActionResult()
        object BookingCancelled : ActionResult()
        object BidPlaced : ActionResult()
        object BidCancelled : ActionResult()
        object BidApproved : ActionResult()
        object BidRejected : ActionResult()
        object BidUpdated : ActionResult()
        data class Error(val message: String) : ActionResult()
        data class ErrorRes(@StringRes val resId: Int) : ActionResult()
    }

    companion object {
        const val EXTRA_CAR_ID = "extra_car_id"
        const val EXTRA_ENTRY_CONTEXT = "extra_entry_context"
        const val EXTRA_BOOKING_ID = "extra_booking_id"

        const val CONTEXT_MARKETPLACE = "MARKETPLACE"
        const val CONTEXT_MY_CARS = "MY_CARS"
        const val CONTEXT_MY_LISTINGS = "MY_LISTINGS"
        const val CONTEXT_MY_RENTALS = "MY_RENTALS"
    }
}
