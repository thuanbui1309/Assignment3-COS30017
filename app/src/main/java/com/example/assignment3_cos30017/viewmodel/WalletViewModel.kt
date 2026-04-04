package com.example.assignment3_cos30017.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3_cos30017.data.model.Transaction
import com.example.assignment3_cos30017.data.network.SePayConfig
import com.example.assignment3_cos30017.data.repository.PaymentRepository
import com.example.assignment3_cos30017.data.repository.WalletRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WalletViewModel : ViewModel() {

    private val walletRepository = WalletRepository()
    private val paymentRepository = PaymentRepository()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _balance = MutableLiveData<Int>()
    val balance: LiveData<Int> = _balance

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _totalSpent = MutableLiveData<Int>()
    val totalSpent: LiveData<Int> = _totalSpent

    private val _totalTopUp = MutableLiveData<Int>()
    val totalTopUp: LiveData<Int> = _totalTopUp

    private val _topUpResult = MutableLiveData<TopUpState?>()
    val topUpResult: LiveData<TopUpState?> = _topUpResult

    private var pollingJob: Job? = null

    init {
        if (userId.isNotEmpty()) {
            observeBalance()
            observeTransactions()
        }
    }

    private fun observeBalance() {
        viewModelScope.launch {
            walletRepository.getBalance(userId).collectLatest { _balance.postValue(it) }
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            walletRepository.getTransactions(userId).collectLatest { txns ->
                _transactions.postValue(txns)
                // "Spent" should match what user actually paid out.
                // For direct rent flow, we record RENTAL_PAYMENT.
                // For bid flow, funds are held (BID_HOLD) then partially/fully refunded (BID_REFUND) if bid is cancelled/rejected.
                val rentalSpent = txns
                    .filter { it.type == Transaction.TYPE_RENTAL_PAYMENT && it.amount < 0 }
                    .sumOf { kotlin.math.abs(it.amount) }
                val bidHeld = txns
                    .filter { it.type == Transaction.TYPE_BID_HOLD && it.amount < 0 }
                    .sumOf { kotlin.math.abs(it.amount) }
                val bidRefunded = txns
                    .filter { it.type == Transaction.TYPE_BID_REFUND && it.amount > 0 }
                    .sumOf { it.amount }

                val bidNetSpent = (bidHeld - bidRefunded).coerceAtLeast(0)
                _totalSpent.postValue(rentalSpent + bidNetSpent)
                _totalTopUp.postValue(
                    txns.filter { it.type == Transaction.TYPE_TOP_UP }.sumOf { it.amount }
                )
            }
        }
    }

    fun confirmTopUp(creditAmount: Int, reference: String) {
        pollingJob?.cancel()
        _topUpResult.value = TopUpState.Polling

        pollingJob = viewModelScope.launch {
            try {
                val expectedVnd = creditAmount.toLong() * SePayConfig.CREDIT_TO_VND
                val vndAmount = paymentRepository.pollForPayment(
                    reference = reference,
                    expectedAmountInVnd = expectedVnd,
                    maxAttempts = POLL_MAX_ATTEMPTS,
                    intervalMs = POLL_INTERVAL_MS
                )
                if (vndAmount != null) {
                    val credits = (vndAmount / SePayConfig.CREDIT_TO_VND).toInt()
                    val finalCredits = if (credits > 0) credits else creditAmount
                    walletRepository.topUp(
                        userId, finalCredits,
                        "Top-up $finalCredits credits via SePay",
                        reference
                    )
                    _topUpResult.postValue(TopUpState.Success(finalCredits))
                } else {
                    _topUpResult.postValue(TopUpState.Timeout)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _topUpResult.postValue(TopUpState.Error)
            }
        }
    }

    fun cancelPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun clearTopUpResult() {
        _topUpResult.value = null
    }

    sealed class TopUpState {
        object Polling : TopUpState()
        data class Success(val credits: Int) : TopUpState()
        object Timeout : TopUpState()
        object Error : TopUpState()
    }

    companion object {
        const val POLL_MAX_ATTEMPTS = 120
        const val POLL_INTERVAL_MS = 5000L
    }
}
