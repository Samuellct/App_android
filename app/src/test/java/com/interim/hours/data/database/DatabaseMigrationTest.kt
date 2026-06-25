package com.interim.hours.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.interim.hours.di.DatabaseModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    @Test
    fun testMigration1To2() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_database"

        // 1. Create database in version 1 schema manually
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create missions table at version 1
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `missions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `syncId` TEXT NOT NULL,
                            `company` TEXT NOT NULL,
                            `agency` TEXT NOT NULL,
                            `hourlyRate` REAL NOT NULL,
                            `siteAddress` TEXT NOT NULL,
                            `colorHex` TEXT NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `nightStartHour` INTEGER NOT NULL,
                            `nightEndHour` INTEGER NOT NULL,
                            `nightRatePercentage` REAL NOT NULL,
                            `hasIfmIccp` INTEGER NOT NULL
                        )
                    """.trimIndent())

                    // Create mission_bonuses table
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `mission_bonuses` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `missionId` INTEGER NOT NULL,
                            `name` TEXT NOT NULL,
                            `defaultAmount` REAL NOT NULL,
                            FOREIGN KEY(`missionId`) REFERENCES `missions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_mission_bonuses_missionId` ON `mission_bonuses` (`missionId`)")

                    // Create work_days table
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `work_days` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `missionId` INTEGER NOT NULL,
                            `dateMillis` INTEGER NOT NULL,
                            `startTimeMillis` INTEGER NOT NULL,
                            `endTimeMillis` INTEGER NOT NULL,
                            `breakMinutes` INTEGER NOT NULL,
                            `comment` TEXT NOT NULL,
                            `syncId` TEXT NOT NULL,
                            FOREIGN KEY(`missionId`) REFERENCES `missions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_work_days_missionId` ON `work_days` (`missionId`)")

                    // Create work_day_bonuses table
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `work_day_bonuses` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `workDayId` INTEGER NOT NULL,
                            `name` TEXT NOT NULL,
                            `amount` REAL NOT NULL,
                            FOREIGN KEY(`workDayId`) REFERENCES `work_days`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_work_day_bonuses_workDayId` ON `work_day_bonuses` (`workDayId`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = helperFactory.create(config)
        val writeDb = openHelper.writableDatabase

        // 2. Insert test data in version 1 database
        writeDb.execSQL("""
            INSERT INTO missions (syncId, company, agency, hourlyRate, siteAddress, colorHex, isActive, nightStartHour, nightEndHour, nightRatePercentage, hasIfmIccp)
            VALUES ('sync-id-123', 'Company A', 'Agency X', 15.5, '123 Main St', '#FF0000', 1, 21, 6, 10.0, 1)
        """.trimIndent())
        openHelper.close()

        // 3. Open the database using Room version 2 and run migration MIGRATION_1_2
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(DatabaseModule.MIGRATION_1_2)
            .build()

        // Trigger database opening and migration execution
        val missionDao = db.missionDao()
        val missions = missionDao.getMissionsWithBonusesFlow().first()

        // 4. Verify that data survived and new columns were created with defaults
        assertEquals(1, missions.size)
        val migratedMission = missions[0].mission
        assertEquals("sync-id-123", migratedMission.syncId)
        assertEquals("Company A", migratedMission.company)
        assertEquals(15.5, migratedMission.hourlyRate, 0.0)

        // Verify default values for migrated columns
        assertEquals(true, migratedMission.hasWeeklyOvertime)
        assertEquals(35.0, migratedMission.weeklyOvertimeThreshold, 0.0)
        assertEquals(25.0, migratedMission.overtimeRate1Percentage, 0.0)
        assertEquals(50.0, migratedMission.overtimeRate2Percentage, 0.0)

        db.close()
    }
}
