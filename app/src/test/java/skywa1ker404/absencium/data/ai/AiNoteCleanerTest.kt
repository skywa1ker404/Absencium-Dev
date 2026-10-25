package skywa1ker404.absencium.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AiNoteCleanerTest {

    @Test
    fun fractionsAndPowersBecomePlainSymbols() {
        assertEquals("a² + b² = c²", AiNoteCleaner.math("a^{2} + b^{2} = c^{2}"))
        assertEquals("(x + 1)/(x - 1)", AiNoteCleaner.math("\\frac{x + 1}{x - 1}"))
        assertEquals("½", AiNoteCleaner.math("\\frac{1}{2}"))
    }

    @Test
    fun rootsAndGreekLettersAreConverted() {
        assertEquals("x = (-b ± √(b² - 4ac))/2a", AiNoteCleaner.math("x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}"))
        assertEquals("Δ = θ × π", AiNoteCleaner.math("\\Delta = \\theta \\times \\pi"))
    }

    @Test
    fun subscriptsWork() {
        assertEquals("H₂O and CO₂", AiNoteCleaner.math("H_2O and CO_2"))
    }

    @Test
    fun dollarMathIsUnwrappedAndDisplayMathIsCentred() {
        val out = AiNoteCleaner.clean("The area is $\\pi r^2$.\n\n$$E = mc^2$$\n\nDone")
        assertEquals("The area is π r².\n\n{align:center}E = mc²\n\nDone", out)
    }

    @Test
    fun pricesAreNotTreatedAsMaths() {
        assertEquals("It costs $5 and $10 today.", AiNoteCleaner.clean("It costs $5 and $10 today."))
    }

    @Test
    fun codeFencesAndBackticksDisappear() {
        val out = AiNoteCleaner.clean("Intro\n```kotlin\nval x = 1\n```\nUse `x` here")
        assertFalse(out.contains("`"))
        assertEquals("Intro\nval x = 1\nUse x here", out)
    }

    @Test
    fun listAndHeadingMarksAreNormalised() {
        assertEquals("### Deep\n- one\n- two\n---", AiNoteCleaner.clean("##### Deep\n* one\n• two\n***"))
    }

    @Test
    fun boldIsNotMistakenForABullet() {
        assertEquals("**Bold** start", AiNoteCleaner.clean("**Bold** start"))
    }
}
