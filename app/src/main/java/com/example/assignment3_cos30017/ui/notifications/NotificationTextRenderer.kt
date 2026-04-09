package com.example.assignment3_cos30017.ui.notifications

import android.content.Context
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.AppNotification

class NotificationTextRenderer(private val context: Context) {

    fun render(n: AppNotification): Pair<String, String> {
        return renderTitle(n) to renderBody(n)
    }

    private fun renderTitle(n: AppNotification): String = when (n.type) {
        AppNotification.TYPE_NEW_BID ->
            context.getString(R.string.notif_title_new_bid, n.carTitle.orEmpty())
        AppNotification.TYPE_BID_APPROVED_WINNER ->
            context.getString(R.string.notif_title_bid_won, n.carTitle.orEmpty())
        AppNotification.TYPE_BID_APPROVED_LOSER ->
            context.getString(R.string.notif_title_bid_lost, n.carTitle.orEmpty())
        AppNotification.TYPE_BID_APPROVED_OWNER ->
            context.getString(R.string.notif_title_bid_approved_owner, n.carTitle.orEmpty())
        AppNotification.TYPE_BID_REJECTED_BY_OWNER ->
            context.getString(R.string.notif_title_bid_rejected, n.carTitle.orEmpty())
        AppNotification.TYPE_BID_CANCELLED ->
            context.getString(R.string.notif_title_bid_cancelled, n.carTitle.orEmpty())
        AppNotification.TYPE_BIDS_CANCELLED_BY_OWNER ->
            context.getString(R.string.notif_title_bids_cancelled_by_owner, n.carTitle.orEmpty())
        else -> context.getString(R.string.notifications_title)
    }

    private fun renderBody(n: AppNotification): String = when (n.type) {
        AppNotification.TYPE_NEW_BID -> {
            val name = n.actorName ?: context.getString(R.string.notif_unknown_user)
            val amount = n.amount ?: 0
            val days = n.rentalDays ?: 0
            context.getString(R.string.notif_body_new_bid, name, amount, days)
        }
        AppNotification.TYPE_BID_APPROVED_WINNER -> {
            val amount = n.amount ?: 0
            val days = n.rentalDays ?: 0
            context.getString(R.string.notif_body_bid_won, amount, days)
        }
        AppNotification.TYPE_BID_APPROVED_LOSER -> {
            val winner = n.actorName ?: context.getString(R.string.notif_unknown_user)
            context.getString(R.string.notif_body_bid_lost, winner)
        }
        AppNotification.TYPE_BID_APPROVED_OWNER -> {
            val winner = n.actorName ?: context.getString(R.string.notif_unknown_user)
            val amount = n.amount ?: 0
            val days = n.rentalDays ?: 0
            context.getString(R.string.notif_body_bid_approved_owner, winner, amount, days)
        }
        AppNotification.TYPE_BID_REJECTED_BY_OWNER -> {
            val owner = n.actorName ?: context.getString(R.string.notif_unknown_user)
            val amount = n.amount ?: 0
            val days = n.rentalDays ?: 0
            context.getString(R.string.notif_body_bid_rejected, owner, amount, days)
        }
        AppNotification.TYPE_BID_CANCELLED -> {
            val name = n.actorName ?: context.getString(R.string.notif_unknown_user)
            context.getString(R.string.notif_body_bid_cancelled, name)
        }
        AppNotification.TYPE_BIDS_CANCELLED_BY_OWNER ->
            context.getString(R.string.notif_body_bids_cancelled_by_owner)
        else -> ""
    }
}

