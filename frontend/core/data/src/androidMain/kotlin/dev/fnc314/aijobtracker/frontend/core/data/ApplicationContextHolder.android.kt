package dev.fnc314.aijobtracker.frontend.core.data

import android.app.Application

/**
 * Static holder maintaining application context reference for Android platform services.
 */
public object ApplicationContextHolder {
    /** Application context instance. */
    public var application: Application? = null
}
