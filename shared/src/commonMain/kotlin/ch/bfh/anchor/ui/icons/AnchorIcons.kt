package ch.bfh.anchor.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The Material icons this app uses, vendored as [ImageVector]s.
 *
 * androidx.compose.material:material-icons-core has no Kotlin Multiplatform build and the
 * JetBrains port was discontinued after 1.7.3, so the icons cannot be taken from a dependency
 * in common code. The path data below is the official 24dp Material Icons (filled) outline,
 * unchanged, so the icons render exactly as they did on Android.
 */
object AnchorIcons {

    val Home: ImageVector by lazy { homeIcon }
    val Favorite: ImageVector by lazy { favoriteIcon }
    val MenuBook: ImageVector by lazy { menuBookIcon }
    val Settings: ImageVector by lazy { settingsIcon }
    val Close: ImageVector by lazy { closeIcon }
    val Add: ImageVector by lazy { addIcon }
    val Delete: ImageVector by lazy { deleteIcon }
    val Edit: ImageVector by lazy { editIcon }
    val Bookmark: ImageVector by lazy { bookmarkIcon }
    val BookmarkBorder: ImageVector by lazy { bookmarkBorderIcon }
    val KeyboardArrowRight: ImageVector by lazy { keyboardArrowRightIcon }
}

private fun materialIcon(
    name: String,
    autoMirror: Boolean,
    pathData: Array<String>
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    autoMirror = autoMirror
).apply {
    pathData.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
}.build()

private val homeIcon: ImageVector = materialIcon(
    name = "Home",
    autoMirror = false,
    pathData = arrayOf(
            "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"
    )
)
private val favoriteIcon: ImageVector = materialIcon(
    name = "Favorite",
    autoMirror = false,
    pathData = arrayOf(
            "m12 21.35-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
    )
)
private val menuBookIcon: ImageVector = materialIcon(
    name = "MenuBook",
    autoMirror = false,
    pathData = arrayOf(
            "M21 5c-1.11-.35-2.33-.5-3.5-.5-1.95 0-4.05.4-5.5 1.5-1.45-1.1-3.55-1.5-5.5-1.5S2.45 4.9 1 6v14.65c0 .25.25.5.5.5.1 0 .15-.05.25-.05C3.1 20.45 5.05 20 6.5 20c1.95 0 4.05.4 5.5 1.5 1.35-.85 3.8-1.5 5.5-1.5 1.65 0 3.35.3 4.75 1.05.1.05.15.05.25.05.25 0 .5-.25.5-.5V6c-.6-.45-1.25-.75-2-1zm0 13.5c-1.1-.35-2.3-.5-3.5-.5-1.7 0-4.15.65-5.5 1.5V8c1.35-.85 3.8-1.5 5.5-1.5 1.2 0 2.4.15 3.5.5v11.5z",
            "M17.5 10.5c.88 0 1.73.09 2.5.26V9.24c-.79-.15-1.64-.24-2.5-.24-1.7 0-3.24.29-4.5.83v1.66c1.13-.64 2.7-.99 4.5-.99zM13 12.49v1.66c1.13-.64 2.7-.99 4.5-.99.88 0 1.73.09 2.5.26V11.9c-.79-.15-1.64-.24-2.5-.24-1.7 0-3.24.3-4.5.83zm4.5 1.84c-1.7 0-3.24.29-4.5.83v1.66c1.13-.64 2.7-.99 4.5-.99.88 0 1.73.09 2.5.26v-1.52c-.79-.16-1.64-.24-2.5-.24z"
    )
)
private val settingsIcon: ImageVector = materialIcon(
    name = "Settings",
    autoMirror = false,
    pathData = arrayOf(
            "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.488.488 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"
    )
)
private val closeIcon: ImageVector = materialIcon(
    name = "Close",
    autoMirror = false,
    pathData = arrayOf(
            "M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
    )
)
private val addIcon: ImageVector = materialIcon(
    name = "Add",
    autoMirror = false,
    pathData = arrayOf(
            "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"
    )
)
private val deleteIcon: ImageVector = materialIcon(
    name = "Delete",
    autoMirror = false,
    pathData = arrayOf(
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"
    )
)
private val editIcon: ImageVector = materialIcon(
    name = "Edit",
    autoMirror = false,
    pathData = arrayOf(
            "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a.996.996 0 0 0 0-1.41l-2.34-2.34a.996.996 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"
    )
)
private val bookmarkIcon: ImageVector = materialIcon(
    name = "Bookmark",
    autoMirror = false,
    pathData = arrayOf(
            "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z"
    )
)
private val bookmarkBorderIcon: ImageVector = materialIcon(
    name = "BookmarkBorder",
    autoMirror = false,
    pathData = arrayOf(
            "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15-5-2.18L7 18V5h10v13z"
    )
)
private val keyboardArrowRightIcon: ImageVector = materialIcon(
    name = "KeyboardArrowRight",
    autoMirror = true,
    pathData = arrayOf(
            "M8.59 16.59 13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"
    )
)
