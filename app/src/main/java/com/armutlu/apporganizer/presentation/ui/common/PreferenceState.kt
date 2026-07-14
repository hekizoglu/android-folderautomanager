package com.armutlu.apporganizer.presentation.ui.common

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.armutlu.apporganizer.utils.AppPrefs

@Composable
fun rememberBooleanPreferenceState(
    context: Context,
    key: String,
    read: () -> Boolean
): MutableState<Boolean> {
    val state = remember(key) { mutableStateOf(read()) }
    DisposableEffect(context, key) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) state.value = read()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        state.value = read()
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

@Composable
fun rememberStringPreferenceState(
    context: Context,
    key: String,
    read: () -> String
): MutableState<String> {
    val state = remember(key) { mutableStateOf(read()) }
    DisposableEffect(context, key) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) state.value = read()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        state.value = read()
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}
