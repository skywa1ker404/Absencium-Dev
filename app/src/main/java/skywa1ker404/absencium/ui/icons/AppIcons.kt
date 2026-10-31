package skywa1ker404.absencium.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The app's icon set: solid glyphs drawn from 24 by 24 path data, so every icon is a single flat shape that takes
 * its colour from the theme instead of an emoji's own colours. The Material icons are Google's, Apache-2.0
 * (github.com/google/material-design-icons, filled style, path data copied unchanged); see docs/THIRD_PARTY_LICENSES.md.
 * The remaining glyphs are listed there too.
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
            "M19,4h-1V2h-2v2H8V2H6v2H5C3.89,4,3.01,4.9,3.01,6L3,20c0,1.1,0.89,2,2,2h14c1.1,0,2-0.9,2-2V6C21,4.9,20.1,4,19,4z M19,20 H5V10h14V20z M9,14H7v-2h2V14z M13,14h-2v-2h2V14z M17,14h-2v-2h2V14z M9,18H7v-2h2V18z M13,18h-2v-2h2V18z M17,18h-2v-2h2V18z",
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
            "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
        )
    }

    val Add: ImageVector by lazy {
        glyph(
            "Add",
            "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
        )
    }

    val SkipNext: ImageVector by lazy {
        glyph("SkipNext", "M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z")
    }

    /** A circle with its right half cut out: the light and dark theme-mode glyph. */
    val Contrast: ImageVector by lazy {
        glyph("Contrast", "M12,22c5.52,0,10-4.48,10-10S17.52,2,12,2S2,6.48,2,12S6.48,22,12,22z M13,4.07c3.94,0.49,7,3.85,7,7.93s-3.05,7.44-7,7.93 V4.07z")
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
            "M12,2C6.49,2,2,6.49,2,12s4.49,10,10,10c1.38,0,2.5-1.12,2.5-2.5c0-0.61-0.23-1.2-0.64-1.67c-0.08-0.1-0.13-0.21-0.13-0.33 c0-0.28,0.22-0.5,0.5-0.5H16c3.31,0,6-2.69,6-6C22,6.04,17.51,2,12,2z M17.5,13c-0.83,0-1.5-0.67-1.5-1.5c0-0.83,0.67-1.5,1.5-1.5 s1.5,0.67,1.5,1.5C19,12.33,18.33,13,17.5,13z M14.5,9C13.67,9,13,8.33,13,7.5C13,6.67,13.67,6,14.5,6S16,6.67,16,7.5 C16,8.33,15.33,9,14.5,9z M5,11.5C5,10.67,5.67,10,6.5,10S8,10.67,8,11.5C8,12.33,7.33,13,6.5,13S5,12.33,5,11.5z M11,7.5 C11,8.33,10.33,9,9.5,9S8,8.33,8,7.5C8,6.67,8.67,6,9.5,6S11,6.67,11,7.5z",
        )
    }

    val Storage: ImageVector by lazy {
        glyph(
            "Storage",
            "M2 20h20v-4H2v4zm2-3h2v2H4v-2zM2 4v4h20V4H2zm4 3H4V5h2v2zm-4 7h20v-4H2v4zm2-3h2v2H4v-2z",
        )
    }

    val AutoAwesome: ImageVector by lazy {
        glyph(
            "AutoAwesome",
            "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z",
        )
    }

    val Science: ImageVector by lazy {
        glyph(
            "Science",
            "M19.8,18.4L14,10.67V6.5l1.35-1.69C15.61,4.48,15.38,4,14.96,4H9.04C8.62,4,8.39,4.48,8.65,4.81L10,6.5v4.17L4.2,18.4 C3.71,19.06,4.18,20,5,20h14C19.82,20,20.29,19.06,19.8,18.4z",
        )
    }

    val Tune: ImageVector by lazy {
        glyph(
            "Tune",
            "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z",
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
            "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
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
            "M3,10h11v2H3V10z M3,8h11V6H3V8z M3,16h7v-2H3V16z M18.01,12.87l0.71-0.71c0.39-0.39,1.02-0.39,1.41,0l0.71,0.71 c0.39,0.39,0.39,1.02,0,1.41l-0.71,0.71L18.01,12.87z M17.3,13.58l-5.3,5.3V21h2.12l5.3-5.3L17.3,13.58z",
        )
    }

    val Check: ImageVector by lazy { glyph("Check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z") }

    val ExpandMore: ImageVector by lazy { glyph("ExpandMore", "M16.59,8.59L12,13.17 7.41,8.59 6,10l6,6 6,-6z") }

    val Info: ImageVector by lazy {
        glyph("Info", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z")
    }

    val History: ImageVector by lazy {
        glyph(
            "History",
            "M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z",
        )
    }

    val PlayCircle: ImageVector by lazy {
        glyph("PlayCircle", "M12,2C6.48,2,2,6.48,2,12s4.48,10,10,10s10-4.48,10-10S17.52,2,12,2z M9.5,16.5v-9l7,4.5L9.5,16.5z")
    }

    val OpenInNew: ImageVector by lazy {
        glyph(
            "OpenInNew",
            "M19,19H5V5h7V3H5c-1.11,0 -2,0.9 -2,2v14c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2v-7h-2v7zM14,3v2h3.59l-9.83,9.83 1.41,1.41L19,6.41V10h2V3h-7z",
        )
    }

    val Image: ImageVector by lazy {
        glyph(
            "Image",
            "M21,19V5c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2zM8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z",
        )
    }

    val PhotoCamera: ImageVector by lazy {
        glyph(
            "PhotoCamera",
            "M12,12m-3.2,0a3.2,3.2 0,1 1,6.4 0a3.2,3.2 0,1 1,-6.4 0 M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z",
        )
    }

    val Stop: ImageVector by lazy { glyph("Stop", "M7,7h10v10H7z") }

    val DocumentScanner: ImageVector by lazy {
        glyph("DocumentScanner", "M3,3h6v2H5v4H3V3zM15,3h6v6h-2V5h-4V3zM3,15h2v4h4v2H3v-6zM19,15h2v6h-6v-2h4v-4zM7,11h10v2H7z")
    }

    val AudioFile: ImageVector by lazy {
        glyph("AudioFile", "M4,10h2v4H4zM8,6h2v12H8zM12,3h2v18h-2zM16,7h2v10h-2zM20,10h2v4h-2z")
    }

    val Crop: ImageVector by lazy {
        glyph("Crop", "M7,2h2v15h13v2h-3v3h-2v-3H7V2zM2,7h5v2H2V7zM11,5h8v8h-2V7h-6V5z")
    }

    val FlipHorizontal: ImageVector by lazy {
        glyph("FlipHorizontal", "M11,3h2v18h-2zM3,19L9,19L9,5zM21,19h-6V5z")
    }

    val FlipVertical: ImageVector by lazy {
        glyph("FlipVertical", "M3,11h18v2H3zM5,9L12,3l7,6zM5,15h14l-7,6z")
    }

    val RotateRight: ImageVector by lazy {
        glyph(
            "RotateRight",
            "M15.55,5.55L11,1v3.07C7.06,4.56 4,7.92 4,12s3.05,7.44 7,7.93v-2.02c-2.84,-0.48 -5,-2.94 -5,-5.91s2.16,-5.43 5,-5.91V10l4.55,-4.45zM19.93,11c-0.17,-1.39 -0.72,-2.73 -1.62,-3.89l-1.42,1.42c0.54,0.75 0.88,1.6 1.02,2.47h2.02zM13,17.9v2.02c1.39,-0.17 2.74,-0.71 3.9,-1.61l-1.44,-1.44c-0.75,0.54 -1.59,0.89 -2.46,1.03zM16.89,15.48l1.42,1.41c0.9,-1.16 1.45,-2.5 1.62,-3.89h-2.02c-0.14,0.87 -0.48,1.72 -1.02,2.48z",
        )
    }

    val OpenWith: ImageVector by lazy {
        glyph("OpenWith", "M12,2L8,6h3v4H7V7L3,11l4,4v-3h4v4H8l4,4l4,-4h-3v-4h4v3l4,-4l-4,-4v3h-4V6h3z")
    }

    val Opacity: ImageVector by lazy {
        glyph("Opacity", "M12,3C12,3 5,10.5 5,15a7,7 0,0 0,14 0C19,10.5 12,3 12,3zM12,19a4,4 0,0 1,-4 -4c0,-1.9 1.9,-4.4 4,-6.9z")
    }

    val Eraser: ImageVector by lazy {
        glyph("Eraser", "M16.2,3.6L4,15.8a2,2 0,0 0,0 2.8l1.4,1.4h6l8.4,-8.4a2,2 0,0 0,0 -2.8L19,6.2a2,2 0,0 0,-2.8 -2.6zM9.5,18L6,14.5l5,-5L14.5,13z")
    }

    val ArrowUp: ImageVector by lazy { glyph("ArrowUp", "M12,4l-7,7h5v9h4v-9h5z") }

    val ArrowDown: ImageVector by lazy { glyph("ArrowDown", "M12,20l7,-7h-5V4h-4v9H5z") }

    val Remove: ImageVector by lazy { glyph("Remove", "M5,11h14v2H5z") }

    val Fullscreen: ImageVector by lazy {
        glyph("Fullscreen", "M4,4h6v2H6v4H4V4zM14,4h6v6h-2V6h-4V4zM4,14h2v4h4v2H4v-6zM18,14h2v6h-6v-2h4v-4z")
    }

    val SizeSmall: ImageVector by lazy { glyph("SizeSmall", "M8,9h8v6H8zM9.5,10.5v3h5v-3z") }

    val SizeMedium: ImageVector by lazy { glyph("SizeMedium", "M6,7h12v10H6zM7.5,8.5v7h9v-7z") }

    val SizeLarge: ImageVector by lazy { glyph("SizeLarge", "M4,5h16v14H4zM5.5,6.5v11h13v-11z") }

    val SizeFull: ImageVector by lazy { glyph("SizeFull", "M2,4h20v16H2zM3.5,5.5v13h17v-13z") }

    val ExtractText: ImageVector by lazy {
        glyph("ExtractText", "M3,3h5v2H5v3H3V3zM16,3h5v5h-2V5h-3V3zM3,16h2v3h3v2H3v-5zM19,16h2v5h-5v-2h3v-3zM7,9h10v2H7zM7,13h7v2H7z")
    }

    val Transcribe: ImageVector by lazy {
        glyph("Transcribe", "M4,4h16v12H8l-4,4V4zM7,8h10v2H7zM7,12h7v2H7z")
    }

    val AddRow: ImageVector by lazy {
        glyph("AddRow", "M3,3h18v9H3zM5,5v5h14V5zM11,14h2v3h3v2h-3v3h-2v-3H8v-2h3z")
    }

    val RemoveRow: ImageVector by lazy {
        glyph("RemoveRow", "M3,3h18v9H3zM5,5v5h14V5zM8,15h8v2H8z")
    }

    val AddColumn: ImageVector by lazy {
        glyph("AddColumn", "M3,3h9v18H3zM5,5v14h5V5zM14,11h3V8h2v3h3v2h-3v3h-2v-3h-3z")
    }

    val RemoveColumn: ImageVector by lazy {
        glyph("RemoveColumn", "M3,3h9v18H3zM5,5v14h5V5zM14,11h8v2h-8z")
    }

    val Mic: ImageVector by lazy {
        glyph(
            "Mic",
            "M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z",
        )
    }

    val Description: ImageVector by lazy {
        glyph(
            "Description",
            "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z",
        )
    }

    val Edit: ImageVector by lazy {
        glyph(
            "Edit",
            "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z",
        )
    }

    val Delete: ImageVector by lazy {
        glyph("Delete", "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z")
    }

    // The glyphs below are Google's Material icons (Apache-2.0), like the ones above.
    val Undo: ImageVector by lazy {
        glyph("Undo", "M12.5,8c-2.65,0 -5.05,0.99 -6.9,2.6L2,7v9h9l-3.62,-3.62c1.39,-1.16 3.16,-1.88 5.12,-1.88 3.54,0 6.55,2.31 7.6,5.5l2.37,-0.78C21.08,11.03 17.15,8 12.5,8z")
    }

    val Menu: ImageVector by lazy {
        glyph("Menu", "M3,18h18v-2L3,16v2zM3,13h18v-2L3,11v2zM3,6v2h18L21,6L3,6z")
    }

    val ContentCopy: ImageVector by lazy {
        glyph("ContentCopy", "M16,1L4,1c-1.1,0 -2,0.9 -2,2v14h2L4,3h12L16,1zM19,5L8,5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2L21,7c0,-1.1 -0.9,-2 -2,-2zM19,21L8,21L8,7h11v14z")
    }

    val PushPin: ImageVector by lazy {
        glyph("PushPin", "M16,9V4l1,0c0.55,0 1,-0.45 1,-1v0c0,-0.55 -0.45,-1 -1,-1H7C6.45,2 6,2.45 6,3v0c0,0.55 0.45,1 1,1l1,0v5c0,1.66 -1.34,3 -3,3h0v2h5.97v7l1,1l1,-1v-7H19v-2h0C17.34,12 16,10.66 16,9z")
    }

    val Send: ImageVector by lazy {
        glyph("Send", "M2.01,21L23,12 2.01,3 2,10l15,2 -15,2z")
    }

    val Settings: ImageVector by lazy {
        glyph("Settings", "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z")
    }

    val Refresh: ImageVector by lazy {
        glyph("Refresh", "M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z")
    }

    val Redo: ImageVector by lazy {
        glyph("Redo", "M18.4,10.6C16.55,8.99 14.15,8 11.5,8c-4.65,0 -8.58,3.03 -9.96,7.22L3.9,16c1.05,-3.19 4.05,-5.5 7.6,-5.5 1.95,0 3.73,0.72 5.12,1.88L13,16h9V7l-3.6,3.6z")
    }

    val MoreVert: ImageVector by lazy {
        glyph("MoreVert", "M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z")
    }

    val CheckBox: ImageVector by lazy {
        glyph("CheckBox", "M19,3H5c-1.11,0 -2,0.9 -2,2v14c0,1.1 0.89,2 2,2h14c1.11,0 2,-0.9 2,-2V5c0,-1.1 -0.89,-2 -2,-2zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z")
    }

    val AlignLeft: ImageVector by lazy {
        glyph("AlignLeft", "M15,15H3v2h12v-2zM15,7H3v2h12V7zM3,13h18v-2H3v2zM3,21h18v-2H3v2zM3,3v2h18V3H3z")
    }

    val ViewList: ImageVector by lazy {
        glyph("ViewList", "M3,14h4v-4H3v4zM3,19h4v-4H3v4zM3,9h4V5H3v4zM8,14h13v-4H8v4zM8,19h13v-4H8v4zM8,5v4h13V5H8z")
    }

    val ViewAgenda: ImageVector by lazy {
        glyph("ViewAgenda", "M20,13H3c-0.55,0 -1,0.45 -1,1v6c0,0.55 0.45,1 1,1h17c0.55,0 1,-0.45 1,-1v-6c0,-0.55 -0.45,-1 -1,-1zM20,3H3c-0.55,0 -1,0.45 -1,1v6c0,0.55 0.45,1 1,1h17c0.55,0 1,-0.45 1,-1V4c0,-0.55 -0.45,-1 -1,-1z")
    }

    val ListBulleted: ImageVector by lazy {
        glyph(
            "ListBulleted",
            "M4,10.5c-0.83,0 -1.5,0.67 -1.5,1.5s0.67,1.5 1.5,1.5 1.5,-0.67 1.5,-1.5 -0.67,-1.5 -1.5,-1.5zM4,4.5c-0.83,0 -1.5,0.67 -1.5,1.5S3.17,7.5 4,7.5 5.5,6.83 5.5,6 4.83,4.5 4,4.5zM4,16.5c-0.83,0 -1.5,0.68 -1.5,1.5s0.68,1.5 1.5,1.5 1.5,-0.68 1.5,-1.5 -0.67,-1.5 -1.5,-1.5zM7,19h14v-2H7v2zM7,13h14v-2H7v2zM7,5v2h14V5H7z",
        )
    }

    val FormatBold: ImageVector by lazy {
        glyph(
            "FormatBold",
            "M15.6,10.79c0.97,-0.67 1.65,-1.77 1.65,-2.79 0,-2.26 -1.75,-4 -4,-4H7v14h7.04c2.09,0 3.71,-1.7 3.71,-3.79 0,-1.52 -0.86,-2.82 -2.15,-3.42zM10,6.5h3c0.83,0 1.5,0.67 1.5,1.5s-0.67,1.5 -1.5,1.5h-3v-3zM13.5,15.5H10v-3h3.5c0.83,0 1.5,0.67 1.5,1.5s-0.67,1.5 -1.5,1.5z",
        )
    }

    val FormatItalic: ImageVector by lazy { glyph("FormatItalic", "M10,4v3h2.21l-3.42,8H6v3h8v-3h-2.21l3.42,-8H18V4z") }

    val FormatUnderlined: ImageVector by lazy {
        glyph("FormatUnderlined", "M12,17c3.31,0 6,-2.69 6,-6V3h-2.5v8c0,1.93 -1.57,3.5 -3.5,3.5S8.5,12.93 8.5,11V3H6v8c0,3.31 2.69,6 6,6zM5,19v2h14v-2H5z")
    }

    val FormatStrikethrough: ImageVector by lazy { glyph("FormatStrikethrough", "M10,19h4v-3h-4v3zM5,4v3h5v3h4V7h5V4H5zM3,14h18v-2H3v2z") }

    val FormatAlignCenter: ImageVector by lazy {
        glyph("FormatAlignCenter", "M7,15v2h10v-2H7zM3,21h18v-2H3v2zM3,13h18v-2H3v2zM7,7v2h10V7H7zM3,3v2h18V3H3z")
    }

    val FormatAlignRight: ImageVector by lazy {
        glyph("FormatAlignRight", "M3,21h18v-2H3v2zM9,17h12v-2H9v2zM3,13h18v-2H3v2zM9,9h12V7H9v2zM3,3v2h18V3H3z")
    }

    val FormatListNumbered: ImageVector by lazy {
        glyph(
            "FormatListNumbered",
            "M2,17h2v0.5H3v1h1v0.5H2v1h3v-4H2v1zM3,8h1V4H2v1h1v3zM2,11h1.8L2,13.1v0.9h3v-1H3.2L5,10.9V10H2v1zM7,5v2h14V5H7zM7,19h14v-2H7v2zM7,13h14v-2H7v2z",
        )
    }

    val FormatQuote: ImageVector by lazy { glyph("FormatQuote", "M6,17h3l2,-4V7H5v6h3zM14,17h3l2,-4V7h-6v6h3z") }

    val Code: ImageVector by lazy {
        glyph("Code", "M9.4,16.6L4.8,12l4.6,-4.6L8,6l-6,6 6,6 1.4,-1.4zM14.6,16.6l4.6,-4.6 -4.6,-4.6L16,6l6,6 -6,6 -1.4,-1.4z")
    }

    val Title: ImageVector by lazy { glyph("Title", "M5,4v3h5.5v12h3V7H19V4z") }

    val FormatSize: ImageVector by lazy { glyph("FormatSize", "M9,4v3h5v12h3V7h5V4H9zM3,12h3v7h3v-7h3V9H3v3z") }

    val FormatColorText: ImageVector by lazy {
        glyph(
            "FormatColorText",
            "M2,20h20v4H2v-4zM5.49,17h2.42l1.27,-3.58h5.65L16.09,17h2.42L13.25,3h-2.5L5.49,17zM9.91,11.39l2.03,-5.79h0.12l2.03,5.79H9.91z",
        )
    }

    val Link: ImageVector by lazy {
        glyph(
            "Link",
            "M3.9,12c0,-1.71 1.39,-3.1 3.1,-3.1h4V7H7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5h4v-1.9H7c-1.71,0 -3.1,-1.39 -3.1,-3.1zM8,13h8v-2H8v2zM17,7h-4v1.9h4c1.71,0 3.1,1.39 3.1,3.1s-1.39,3.1 -3.1,3.1h-4V17h4c2.76,0 5,-2.24 5,-5s-2.24,-5 -5,-5z",
        )
    }

    val HorizontalRule: ImageVector by lazy { glyph("HorizontalRule", "M4,11h16v2H4z") }

    val Functions: ImageVector by lazy { glyph("Functions", "M18,4H6v2l6.5,6L6,18v2h12v-3h-7l5,-5 -5,-5h7z") }

    // A marker pen: the pencil above a bar of colour.
    val Highlight: ImageVector by lazy {
        glyph(
            "Highlight",
            "M3,13.25V17h3.75L17.81,5.94l-3.75,-3.75L3,13.25zM20.71,3.04c0.39,-0.39 0.39,-1.02 0,-1.41l-0.34,-0.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83zM2,20h20v3H2z",
        )
    }

    val Brush: ImageVector by lazy {
        glyph(
            "Brush",
            "M7,14c-1.66,0 -3,1.34 -3,3 0,1.31 -1.16,2 -2,2 0.92,1.22 2.49,2 4,2 2.21,0 4,-1.79 4,-4 0,-1.66 -1.34,-3 -3,-3zM20.71,4.63l-1.34,-1.34c-0.39,-0.39 -1.02,-0.39 -1.41,0L9,12.25 11.75,15l8.96,-8.96c0.39,-0.39 0.39,-1.02 0,-1.41z",
        )
    }

    val Table: ImageVector by lazy {
        glyph(
            "Table",
            "M10,10.02h5V21h-5zM17,21h3c1.1,0 2,-0.9 2,-2v-9h-5v11zM20,3L5,3c-1.1,0 -2,0.9 -2,2v3h19L22,5c0,-1.1 -0.9,-2 -2,-2zM3,19c0,1.1 0.9,2 2,2h3L8,10L3,10v9z",
        )
    }

    // The AI panel's icons are drawn for this app from plain shapes (bars, arrows, a star, a bulb).

    /** Lines getting shorter: a summary. */
    val Summarize: ImageVector by lazy { glyph("Summarize", "M4,5h16v2H4zM4,10h16v2H4zM4,15h11v2H4zM4,20h6v2H4z") }

    /** Two arrows pressing in on a line: shorter text. */
    val Shorten: ImageVector by lazy { glyph("Shorten", "M4,11h16v2H4zM12,10L8,6h3V3h2v3h3zM12,14l4,4h-3v3h-2v-3H8z") }

    /** Two arrows moving apart from a line: longer text. */
    val Expand: ImageVector by lazy { glyph("Expand", "M4,11h16v2H4zM12,2l4,4h-3v3h-2V6H8zM12,22l-4,-4h3v-3h2v3h3z") }

    /** A four-point star: polish. */
    val Sparkle: ImageVector by lazy { glyph("Sparkle", "M12,2l2.4,7.6L22,12l-7.6,2.4L12,22l-2.4,-7.6L2,12l7.6,-2.4zM19,2l0.9,2.1L22,5l-2.1,0.9L19,8l-0.9,-2.1L16,5l2.1,-0.9z") }

    /** A check mark over a line: grammar fixed. */
    val Spellcheck: ImageVector by lazy { glyph("Spellcheck", "M8.6,14.2L4.7,10.3L3.3,11.7L8.6,17L20.7,4.9L19.3,3.5zM3,19h18v2H3z") }

    /** A pen with a trailing line: carry on writing. */
    val PenLine: ImageVector by lazy { glyph("PenLine", "M3,17l9.6,-9.6l3.6,3.6L6.6,20.6H3zM14,6l1.8,-1.8a1,1 0 0,1 1.4,0l2.6,2.6a1,1 0 0,1 0,1.4L18,10zM12,21h9v-2h-7z") }

    /** A bulb: a simple explanation. */
    val Lightbulb: ImageVector by lazy { glyph("Lightbulb", "M12,2a7,7 0 0,0 -4,12.7V17h8v-2.3A7,7 0 0,0 12,2zM9,19h6v2H9zM10,22h4v1h-4z") }

    /** Squares with lines: a list of things to do. */
    val Tasks: ImageVector by lazy { glyph("Tasks", "M4,4h5v5H4zM11,5.5h9v2h-9zM4,15h5v5H4zM11,16.5h9v2h-9z") }

    /** A question mark: quiz questions. */
    val Quiz: ImageVector by lazy { glyph("Quiz", "M12,3a5,5 0 0,0 -5,5h2.4a2.6,2.6 0 1,1 4.2,2.1c-1.6,1.2 -2.6,2 -2.6,4V15h2.4v-0.8c0,-1.2 0.7,-1.7 1.8,-2.6A5,5 0 0,0 12,3zM10.8,17.5h2.4v2.4h-2.4z") }

    /** Two arrows going opposite ways: translation. */
    val Translate: ImageVector by lazy { glyph("Translate", "M6,7h10V4l5,4.5L16,13v-3H6zM18,17H8v3l-5,-4.5L8,11v3h10z") }

    /** Three linked dots: share. */
    val Share: ImageVector by lazy { glyph("Share", "M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7l7.05,-4.11c0.54,0.5 1.25,0.81 2.04,0.81 1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3 -3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7L8.04,9.81C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.61 1.31,2.92 2.92,2.92 1.61,0 2.92,-1.31 2.92,-2.92s-1.31,-2.92 -2.92,-2.92z") }

    /** A ribbon: bookmark. */
    val Bookmark: ImageVector by lazy { glyph("Bookmark", "M17,3H7c-1.1,0 -1.99,0.9 -1.99,2L5,21l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2z") }
}
