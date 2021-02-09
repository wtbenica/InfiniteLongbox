package com.wtb.comiccollector

import android.text.Editable
import android.text.TextWatcher

/**
 * A TextWatcher that applies [transformation] to the CharSequence? onTextChanged, with no
 * effects for before- or after- TextChanged
 *
 * @property transformation the action to apply upon onTextChanged
 * @return a TextWatcher that applies transformation
 */
class SimpleTextWatcher(val transformation: (CharSequence?) -> Unit) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        transformation(s)
    }

    override fun afterTextChanged(s: Editable?) {

    }
}