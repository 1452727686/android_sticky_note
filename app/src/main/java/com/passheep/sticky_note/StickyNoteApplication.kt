package com.passheep.sticky_note

import android.app.Application
import androidx.work.Configuration
import com.passheep.sticky_note.data.sync.SyncStartup
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StickyNoteApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var syncStartup: SyncStartup

    override fun onCreate() {
        super.onCreate()
        syncStartup.start()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
