package com.interim.hours.data.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.content.Context
import android.os.ParcelFileDescriptor

class AppBackupAgent : BackupAgent() {

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {
        // No-op: we rely on Auto Backup (onFullBackup)
    }

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        // No-op: we rely on Auto Backup (onRestoreFile)
    }

    override fun onFullBackup(data: FullBackupDataOutput) {
        val sharedPrefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val isBackupEnabled = sharedPrefs.getBoolean("google_backup_enabled", true)

        if (isBackupEnabled) {
            super.onFullBackup(data)
        }
    }
}
