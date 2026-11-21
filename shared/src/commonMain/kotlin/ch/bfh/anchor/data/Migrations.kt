package ch.bfh.anchor.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Real migrations, replacing `fallbackToDestructiveMigration(dropAllTables = true)`. In a therapy
 * journal that option deletes everything the user has written on the next schema change.
 *
 * The CREATE statements are copied verbatim from the exported schemas in `shared/schemas`, so a
 * migrated table is identical to the one a fresh install gets. Version 3 was never exported, so
 * 2 to 3 and 3 to 4 each create whichever table is still missing; every statement is idempotent,
 * so every upgrade path lands on the same schema.
 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(CREATE_THERAPY_NOTES)
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(CREATE_CRISIS_CHECK_INS)
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(CREATE_CRISIS_CHECK_INS)
        connection.execSQL(CREATE_SESSION_QUESTIONS)
    }
}

internal val ANCHOR_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4
)

private const val CREATE_THERAPY_NOTES =
    "CREATE TABLE IF NOT EXISTS `therapy_notes` (`id` TEXT NOT NULL, `date` INTEGER NOT NULL, " +
        "`body` TEXT NOT NULL, `tags` TEXT NOT NULL, `flaggedForNextSession` INTEGER NOT NULL, " +
        "PRIMARY KEY(`id`))"

private const val CREATE_CRISIS_CHECK_INS =
    "CREATE TABLE IF NOT EXISTS `crisis_check_ins` (`id` TEXT NOT NULL, `date` INTEGER NOT NULL, " +
        "`feeling` TEXT NOT NULL, `feelingAfter` INTEGER NOT NULL, PRIMARY KEY(`id`))"

private const val CREATE_SESSION_QUESTIONS =
    "CREATE TABLE IF NOT EXISTS `session_questions` (`id` TEXT NOT NULL, `text` TEXT NOT NULL, " +
        "`date` INTEGER NOT NULL, `answered` INTEGER NOT NULL, PRIMARY KEY(`id`))"
