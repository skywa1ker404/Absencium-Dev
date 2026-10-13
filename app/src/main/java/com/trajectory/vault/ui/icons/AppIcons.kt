package com.trajectory.vault.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The app's icon set: solid Material-style glyphs drawn from 24 by 24 path data, so every icon
 * is a single flat shape that takes its colour from the theme instead of an emoji's own colours.
 */
object AppIcons {

    private fun glyph(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = SolidColor(Color.Black),
        ).build()

    val Home: ImageVector by lazy {
        glyph(
            "Home",
            "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z",
        )
    }

    val CalendarMonth: ImageVector by lazy {
        glyph(
            "CalendarMonth",
            "M19,4h-1V2h-2v2H8V2H6v2H5C3.89,4 3.01,4.9 3.01,6L3,20c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V6C21,4.9 20.1,4 19,4zM19,20H5V10h14V20zM19,8H5V6h14V8z",
        )
    }

    val Folder: ImageVector by lazy {
        glyph(
            "Folder",
            "M10,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2h-8l-2,-2z",
        )
    }

    val Search: ImageVector by lazy {
        glyph(
            "Search",
            "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
        )
    }

    val Add: ImageVector by lazy {
        glyph(
            "Add",
            "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
        )
    }

    val ChevronRight: ImageVector by lazy {
        glyph(
            "ChevronRight",
            "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z",
        )
    }

    val ChevronLeft: ImageVector by lazy {
        glyph(
            "ChevronLeft",
            "M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z",
        )
    }

    val ArrowBack: ImageVector by lazy {
        glyph(
            "ArrowBack",
            "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z",
        )
    }

    val School: ImageVector by lazy {
        glyph(
            "School",
            "M5,13.18v4L12,21l7,-3.82v-4L12,17l-7,-3.82zM12,3L1,9l11,6 9,-4.91V17h2V9L12,3z",
        )
    }

    val Palette: ImageVector by lazy {
        glyph(
            "Palette",
            "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9c0.83,0 1.5,-0.67 1.5,-1.5 0,-0.39 -0.15,-0.74 -0.39,-1.01 -0.23,-0.26 -0.38,-0.61 -0.38,-0.99 0,-0.83 0.67,-1.5 1.5,-1.5L16,17c2.76,0 5,-2.24 5,-5 0,-4.42 -4.03,-8 -9,-8zM6.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S5.67,9 6.5,9 8,9.67 8,10.5 7.33,12 6.5,12zM9.5,8C8.67,8 8,7.33 8,6.5S8.67,5 9.5,5s1.5,0.67 1.5,1.5S10.33,8 9.5,8zM14.5,8c-0.83,0 -1.5,-0.67 -1.5,-1.5S13.67,5 14.5,5s1.5,0.67 1.5,1.5S15.33,8 14.5,8zM17.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S16.67,9 17.5,9s1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5z",
        )
    }

    val Storage: ImageVector by lazy {
        glyph(
            "Storage",
            "M2,20h20v-4L2,16v4zM4,17h2v2L4,19v-2zM2,4v4h20L22,4L2,4zM6,7L4,7L4,5h2v2zM2,14h20v-4L2,10v4zM4,11h2v2L4,13v-2z",
        )
    }

    val AutoAwesome: ImageVector by lazy {
        glyph(
            "AutoAwesome",
            "M19,9l1.25,-2.75L23,5l-2.75,-1.25L19,1l-1.25,2.75L15,5l2.75,1.25L19,9zM19,15l-1.25,2.75L15,19l2.75,1.25L19,23l1.25,-2.75L23,19l-2.75,-1.25L19,15zM11.5,9.5L9,4L6.5,9.5L1,12l5.5,2.5L9,20l2.5,-5.5L17,12l-5.5,-2.5z",
        )
    }

    val Science: ImageVector by lazy {
        glyph(
            "Science",
            "M13,11.33L18,18H6l5,-6.67V6h2M15.96,4H8.04C7.62,4 7.39,4.48 7.65,4.81L9,6.5v4.17L3.2,18.4C2.71,19.06 3.18,20 4,20h16c0.82,0 1.29,-0.94 0.8,-1.6L15,10.67V6.5l1.35,-1.69C16.61,4.48 16.38,4 15.96,4z",
        )
    }

    val Tune: ImageVector by lazy {
        glyph(
            "Tune",
            "M3,17v2h6v-2L3,17zM3,5v2h10L13,5L3,5zM13,21v-2h8v-2h-8v-2h-2v6h2zM7,9v2L3,11v2h4v2h2L9,9L7,9zM21,13v-2L11,11v2h10zM15,9h2L17,7h4L21,5h-4L17,3h-2v6z",
        )
    }

    val Schedule: ImageVector by lazy {
        glyph(
            "Schedule",
            "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z",
        )
    }

    val Book: ImageVector by lazy {
        glyph(
            "Book",
            "M18,2H6c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zM6,4h5v8l-2.5,-1.5L6,12V4z",
        )
    }

    val Person: ImageVector by lazy {
        glyph(
            "Person",
            "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z",
        )
    }

    val Close: ImageVector by lazy {
        glyph(
            "Close",
            "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z",
        )
    }

    val Draft: ImageVector by lazy {
        glyph(
            "Draft",
            "M14,2H6C4.9,2 4,2.9 4,4v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V8L14,2zM13,9V3.5L18.5,9H13zM6,20V4h5v7h7v9H6z",
        )
    }

    val Check: ImageVector by lazy { glyph("Check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z") }

    val ExpandMore: ImageVector by lazy { glyph("ExpandMore", "M16.59,8.59L12,13.17 7.41,8.59 6,10l6,6 6,-6z") }

    val Info: ImageVector by lazy {
        glyph("Info", "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-6h2v6zM13,9h-2L11,7h2v2z")
    }

    val History: ImageVector by lazy {
        glyph(
            "History",
            "M13,3c-4.97,0 -9,4.03 -9,9L1,12l3.89,3.89 0.07,0.14L9,12L6,12c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08L13.5,8L12,8z",
        )
    }

    val PlayCircle: ImageVector by lazy {
        glyph("PlayCircle", "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM10,16.5v-9l6,4.5 -6,4.5z")
    }

    val OpenInNew: ImageVector by lazy {
        glyph(
            "OpenInNew",
            "M19,19H5V5h7V3H5c-1.11,0 -2,0.9 -2,2v14c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2v-7h-2v7zM14,3v2h3.59l-9.83,9.83 1.41,1.41L19,6.41V10h2V3h-7z",
        )
    }
}
