package com.qwibbler.salarm.salah

import android.content.Context
import android.text.InputFilter
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.qwibbler.salarm.R

object RelationalAlarmUiHelper {

    @JvmStatic
    fun setupNumericDropdown(view: MaterialAutoCompleteTextView) {
        val context = view.context
        val customText = context.getString(R.string.relational_alarm_custom)
        val items = listOf("2", "5", "10", "15", "20", "30", "45", "60", customText)

        val adapter = ArrayAdapter(context, R.layout.dropdown_item_relational, items)
        view.setAdapter(adapter)

        // Ensure we don't filter the predefined list when typing
        view.threshold = 100

        // Limit to 3 digits
        view.filters = arrayOf(InputFilter.LengthFilter(3))

        // Show dropdown when clicked
        view.setOnClickListener {
            view.showDropDown()
        }

        view.setOnItemClickListener { _, _, position, _ ->
            val selected = adapter.getItem(position)
            if (selected == customText) {
                view.setText("")
                view.requestFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    @JvmStatic
    fun setupNumericDropdowns(vararg views: MaterialAutoCompleteTextView) {
        views.forEach { setupNumericDropdown(it) }
    }
}
