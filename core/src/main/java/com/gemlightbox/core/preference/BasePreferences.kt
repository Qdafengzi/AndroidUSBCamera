package com.gemlightbox.core.preference

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

abstract class BasePreferences(context: Context) {

    private val MY_PREFERENCES = "CameraPickPreferences"

    protected val sp: SharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
    protected val gson = Gson()

    protected inline fun SharedPreferences.put(body: SharedPreferences.Editor.() -> Unit) {
        val editor = this.edit()
        editor.body()
        editor.apply()
    }
}