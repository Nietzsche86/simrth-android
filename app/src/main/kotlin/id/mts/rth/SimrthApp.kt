package id.mts.rth

import android.app.Application

class SimrthApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Koin DI modules and Coil ImageLoader setup will be hooked in subsequent phases
    }
}
