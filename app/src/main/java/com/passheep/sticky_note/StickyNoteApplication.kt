package com.passheep.sticky_note

import android.app.Application
import com.passheep.sticky_note.data.sync.SyncStartup
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StickyNoteApplication : Application() {

    @Inject
    lateinit var syncStartup: SyncStartup

    override fun onCreate() {
        super.onCreate()
        syncStartup.start()
    }
}
