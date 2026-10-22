package skywa1ker404.absencium.ui.vault

import org.junit.Assert.assertEquals
import org.junit.Test
import skywa1ker404.absencium.ui.vault.markdown.TableGrid

class TableGridTest {
    @Test fun readsCellsAndSkipsTheSeparator() {
        val rows = TableGrid.parse("| a | b |\n| --- | :---: |\n| 1 | 2 |")
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), rows)
    }

    @Test fun shortRowsArePadded() {
        assertEquals(listOf(listOf("a", "b"), listOf("1", "")), TableGrid.parse("| a | b |\n| 1 |"))
    }

    @Test fun writesHeaderSeparatorAndRows() {
        val text = TableGrid.build(listOf(listOf("a", "b"), listOf("1", "2")))
        assertEquals("| a | b |\n| --- | --- |\n| 1 | 2 |", text)
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), TableGrid.parse(text))
    }

    @Test fun barsAndBreaksInACellCannotBreakTheTable() {
        val text = TableGrid.build(listOf(listOf("x|y", "line\nbreak")))
        assertEquals("| x/y | line break |\n| --- | --- |", text)
    }

    @Test fun emptyInputGivesOneEmptyCell() {
        assertEquals(listOf(listOf("")), TableGrid.parse(""))
        assertEquals("|  |\n| --- |", TableGrid.build(emptyList()))
    }
}
