package com.interim.hours.data.backup

import android.app.backup.BackupAgentHelper
import android.app.backup.FullBackupDataOutput
import android.content.Context

class AppBackupAgent : BackupAgentHelper() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        val sharedPrefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val isBackupEnabled = sharedPrefs.getBoolean("google_backup_enabled", true)

        if (isBackupEnabled) {
            super.onFullBackup(data)
        }
    }
}
