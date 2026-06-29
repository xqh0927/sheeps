package com.example.sheeps.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.hjq.toast.Toaster

class ToasterInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Toaster.init(context as android.app.Application)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
