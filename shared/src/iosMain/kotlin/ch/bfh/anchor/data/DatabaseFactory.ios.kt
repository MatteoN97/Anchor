package ch.bfh.anchor.data

import androidx.room.Room
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

fun createDatabase(): AnchorDatabase =
    Room.databaseBuilder<AnchorDatabase>(
        name = "${documentsDirectory()}/$ANCHOR_DATABASE_NAME"
    ).configured()

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun documentsDirectory(): String {
    val url: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    return requireNotNull(url?.path) { "Could not resolve the iOS documents directory" }
}
