package ch.bfh.anchor.data



import androidx.room.ConstructedBy

import androidx.room.Database

import androidx.room.RoomDatabase

import androidx.room.RoomDatabaseConstructor
import ch.bfh.anchor.model.MoodEntry

import androidx.sqlite.driver.bundled.BundledSQLiteDriver

import ch.bfh.anchor.model.CopingTool

import ch.bfh.anchor.model.CrisisCheckIn

import ch.bfh.anchor.model.SessionQuestion

import ch.bfh.anchor.model.TherapyNote



internal const val ANCHOR_DATABASE_NAME = "anchor_database"



@Database(

    entities = [CopingTool::class, TherapyNote::class, CrisisCheckIn::class, SessionQuestion::class, MoodEntry::class],

    version = 5

)

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

 * `fallbackToDestructiveMigration` is carried over from the Android app as-is. It drops all

 * user data on any schema change and should be replaced with real migrations before release.

 */

internal fun RoomDatabase.Builder<AnchorDatabase>.configured(): AnchorDatabase =

    this.setDriver(BundledSQLiteDriver())

        .addMigrations(MIGRATION_1_2, MIGRATION_2_4, MIGRATION_4_5)

        .build()

