package ch.bfh.anchor.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Existing schema snapshots are at versions 1, 2 and 4. These paths retain previous data. */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `therapy_notes` (`id` TEXT NOT NULL, `date` INTEGER NOT NULL, `body` TEXT NOT NULL, `tags` TEXT NOT NULL, `flaggedForNextSession` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
    }
}

internal val MIGRATION_2_4 = object : Migration(2, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `crisis_check_ins` (`id` TEXT NOT NULL, `date` INTEGER NOT NULL, `feeling` TEXT NOT NULL, `feelingAfter` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `session_questions` (`id` TEXT NOT NULL, `text` TEXT NOT NULL, `date` INTEGER NOT NULL, `answered` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
    }
}

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `mood_entries` (`id` TEXT NOT NULL, `recordedAt` INTEGER NOT NULL, `score` INTEGER NOT NULL, `source` TEXT NOT NULL, `promptId` TEXT, PRIMARY KEY(`id`))""")
        connection.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_mood_entries_promptId` ON `mood_entries` (`promptId`)""")
    }
}
