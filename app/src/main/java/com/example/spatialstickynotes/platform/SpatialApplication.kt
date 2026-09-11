package com.example.spatialstickynotes.platform

import android.app.Application
import com.example.spatialstickynotes.data.NoteStore
import com.example.spatialstickynotes.mainApp
import com.pico.spatial.ui.foundation.dsl.launch

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NoteStore.init(this)
        launch(::mainApp)
    }
}
