package ch.bfh.anchor.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Both branches shipped a database version 5 independently:
 * - main v5: journal tags use the unit-separator encoding, no mood_entries table.
 * - mood feature v5: mood_entries exists, journal tags still use comma encoding.
 *
 * Preserve BOTH user data sets when upgrading to the combined schema version 6.
 * This checks the existing schema before creating the missing table; it never wipes data.
 */
internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        val hasMoodTable = connection.prepare(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'mood_entries' LIMIT 1"
        ).use { statement -> statement.step() }

        if (hasMoodTable) {
            // Mood-branch v5 did not have the new tag converter from main's v5.
            connection.execSQL("UPDATE therapy_notes SET tags = replace(tags, ',', char(31))")
        }

        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS `mood_entries` (`id` TEXT NOT NULL, """ +
                """`recordedAt` INTEGER NOT NULL, `score` INTEGER NOT NULL, """ +
                """`source` TEXT NOT NULL, `promptId` TEXT, PRIMARY KEY(`id`))"""
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_mood_entries_promptId` " +
                "ON `mood_entries` (`promptId`)"
        )
    }
}
