package ch.bfh.anchor.data

import android.content.Context
import androidx.room.Room

/** Points at the same file the previous Android build used, so existing data is kept. */
fun createDatabase(context: Context): AnchorDatabase =
    Room.databaseBuilder<AnchorDatabase>(
        context = context.applicationContext,
        name = context.getDatabasePath(ANCHOR_DATABASE_NAME).absolutePath
    ).configured()
