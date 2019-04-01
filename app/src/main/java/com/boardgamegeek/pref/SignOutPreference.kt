package com.boardgamegeek.pref

import android.content.Context
import androidx.preference.DialogPreference
import android.util.AttributeSet
import android.util.TypedValue
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator

class SignOutPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {

    init {
        val typedValue = TypedValue()
        getContext().theme.resolveAttribute(android.R.attr.alertDialogIcon, typedValue, true)
        setDialogIcon(typedValue.resourceId)
        dialogTitle = "$title?"
        dialogLayoutResource = R.layout.widget_dialogpreference_textview
    }

    override fun getDialogMessage(): CharSequence {
        return context.getString(R.string.pref_sync_sign_out_are_you_sure)
    }

    override fun isEnabled(): Boolean {
        return Authenticator.isSignedIn(context)
    }

    fun update() {
        notifyChanged()
    }
}
