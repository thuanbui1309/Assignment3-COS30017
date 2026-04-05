package com.example.assignment3_cos30017.util

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import com.example.assignment3_cos30017.R
import com.google.android.material.appbar.MaterialToolbar

object ToolbarBadgeHelper {

    fun bindActionIconWithBadge(
        activity: AppCompatActivity,
        toolbar: MaterialToolbar,
        menuItemId: Int,
        @DrawableRes iconRes: Int,
        onClick: () -> Unit
    ): BadgeViews? {
        val item = toolbar.menu.findItem(menuItemId) ?: return null
        val actionView = item.actionView ?: return null

        val iv = actionView.findViewById<ImageView>(R.id.iv_icon) ?: return null
        val tv = actionView.findViewById<TextView>(R.id.tv_badge) ?: return null

        iv.setImageResource(iconRes)
        actionView.setOnClickListener { onClick() }

        // Also keep normal menu click path working for accessibility / keyboard.
        item.setOnMenuItemClickListener {
            onClick()
            true
        }
        return BadgeViews(actionView, tv)
    }

    fun renderCount(views: BadgeViews?, count: Int) {
        if (views == null) return
        val c = count.coerceAtLeast(0)
        if (c == 0) {
            views.badgeText.visibility = View.GONE
            return
        }
        views.badgeText.visibility = View.VISIBLE
        views.badgeText.text = if (c > 99) "99+" else c.toString()
    }

    data class BadgeViews(
        val root: View,
        val badgeText: TextView
    )
}

