package com.example.assignment3_cos30017.util

import androidx.appcompat.app.AppCompatActivity
import com.example.assignment3_cos30017.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial

object DialogHelper {

    fun showSettingsBottomSheet(activity: AppCompatActivity) {
        val bottomSheet = BottomSheetDialog(activity)
        bottomSheet.setContentView(R.layout.dialog_settings)

        val switchDarkMode = bottomSheet.findViewById<SwitchMaterial>(R.id.switch_dark_mode)
        val rbEnglish = bottomSheet.findViewById<android.widget.RadioButton>(R.id.rb_english)
        val rbVietnamese = bottomSheet.findViewById<android.widget.RadioButton>(R.id.rb_vietnamese)

        if (switchDarkMode != null) {
            switchDarkMode.isChecked = ThemeHelper.getSavedDarkMode(activity)
            switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                ThemeHelper.applyDarkMode(activity, isChecked)
                bottomSheet.dismiss()
                activity.recreate()
            }
        }

        if (rbEnglish != null && rbVietnamese != null) {
            val currentLang = LocaleHelper.getLanguage(activity)
            if (currentLang == "vi") {
                rbVietnamese.isChecked = true
            } else {
                rbEnglish.isChecked = true
            }

            rbEnglish.setOnClickListener {
                if (currentLang != "en") {
                    LocaleHelper.setLanguage(activity, "en")
                    bottomSheet.dismiss()
                    activity.recreate()
                }
            }

            rbVietnamese.setOnClickListener {
                if (currentLang != "vi") {
                    LocaleHelper.setLanguage(activity, "vi")
                    bottomSheet.dismiss()
                    activity.recreate()
                }
            }
        }

        bottomSheet.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheetInternal = d.findViewById<android.view.View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            if (bottomSheetInternal != null) {
                val behavior = BottomSheetBehavior.from(bottomSheetInternal)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        bottomSheet.show()
    }
}
