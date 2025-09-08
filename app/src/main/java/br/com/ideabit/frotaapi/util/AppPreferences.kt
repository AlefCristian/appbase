package br.com.ideabit.frotaapi.util

import android.annotation.SuppressLint
import android.content.Context

object AppPreferences {
    @SuppressLint("StaticFieldLeak")
    lateinit var userPrefs: UserPreferences

    fun init(context: Context) {
        userPrefs = UserPreferences(context.applicationContext)
    }
}