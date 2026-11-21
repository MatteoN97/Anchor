package ch.bfh.anchor.data

import android.content.Context
import androidx.room.Room

/** A single Room instance ensures the UI sees inserts made by a notification receiver. */
@Volatile private var androidDatabase: AnchorDatabase? = null

/** Keeps the existing Android database file location intact. */
fun createDatabase(context: Context): AnchorDatabase =
    androidDatabase ?: synchronized(AnchorDatabase::class) {
        androidDatabase ?: Room.databaseBuilder<AnchorDatabase>(
            context = context.applicationContext,
            name = context.getDatabasePath(ANCHOR_DATABASE_NAME).absolutePath
        ).configured().also { androidDatabase = it }
    }
