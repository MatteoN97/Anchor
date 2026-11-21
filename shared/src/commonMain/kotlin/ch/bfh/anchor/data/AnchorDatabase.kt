package ch.bfh.anchor.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import ch.bfh.anchor.model.CopingTool
import ch.bfh.anchor.model.CrisisCheckIn
import ch.bfh.anchor.model.MoodEntry
import ch.bfh.anchor.model.SessionQuestion
import ch.bfh.anchor.model.TherapyNote

internal const val ANCHOR_DATABASE_NAME = "anchor_database"

@Database(
    entities = [CopingTool::class, TherapyNote::class, CrisisCheckIn::class, SessionQuestion::class, MoodEntry::class],
    version = 6
)
@TypeConverters(TagListConverter::class)
@ConstructedBy(AnchorDatabaseConstructor::class)
abstract class AnchorDatabase : RoomDatabase() {
    abstract fun copingToolDao(): CopingToolDao
    abstract fun therapyNoteDao(): TherapyNoteDao
    abstract fun crisisCheckInDao(): CrisisCheckInDao
    abstract fun sessionQuestionDao(): SessionQuestionDao
    abstract fun moodEntryDao(): MoodEntryDao
}

// Generated per platform by the Room compiler.
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object AnchorDatabaseConstructor : RoomDatabaseConstructor<AnchorDatabase> {
    override fun initialize(): AnchorDatabase
}

/**
 * Shared configuration applied to the platform-specific builder.
 *
 * There is deliberately no `fallbackToDestructiveMigration` here: this database holds therapy
 * notes, and falling back would delete them. Every version is covered by [ANCHOR_MIGRATIONS]
 * instead, so a missing migration fails loudly at open time rather than silently wiping the
 * journal. Adding a version means adding a migration.
 */
internal fun RoomDatabase.Builder<AnchorDatabase>.configured(): AnchorDatabase =
    this.setDriver(BundledSQLiteDriver())
        .addMigrations(*ANCHOR_MIGRATIONS)
        .build()
