package skywa1ker404.absencium.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiQuizTest {
    private val reply = """
        Here you go, five quick ones.

        ```absencium-quiz
        {"title":"Cells","questions":[
          {"type":"mcq","q":"Powerhouse of the cell?","options":["A) Nucleus","B) Mitochondrion","C) Ribosome","D) Golgi"],"answer":"B","explain":"It makes ATP."},
          {"type":"true_false","q":"Platelets carry oxygen.","answer":"false","explain":"Red cells do."},
          {"type":"short","q":"Basic unit of life?","answer":"The cell","accept":["cell"],"explain":""}
        ]}
        ```
    """.trimIndent()

    @Test fun theQuizBlockIsTakenOutOfTheText() {
        val (text, json) = AiQuiz.extract(reply)
        assertEquals("Here you go, five quick ones.", text)
        assertNotNull(json)
        assertEquals(3, AiQuiz.parse(json!!).size)
        assertEquals("Cells", AiQuiz.title(json))
    }

    @Test fun questionKindsAreReadWithTheirAnswers() {
        val q = AiQuiz.parse(AiQuiz.extract(reply).second!!)
        assertEquals(QuizQuestion.Kind.CHOICE, q[0].kind)
        assertEquals(1, q[0].correct)
        assertEquals("Mitochondrion", q[0].options[1])
        assertEquals(QuizQuestion.Kind.TRUE_FALSE, q[1].kind)
        assertEquals(1, q[1].correct)
        assertEquals(QuizQuestion.Kind.TYPED, q[2].kind)
    }

    @Test fun typedAnswersMatchIgnoringCaseAndPunctuationOtherwiseTheStudentJudges() {
        val q = AiQuiz.parse(AiQuiz.extract(reply).second!!)[2]
        assertEquals(true, AiQuiz.checkTyped(q, "  the CELL. "))
        assertEquals(true, AiQuiz.checkTyped(q, "cell"))
        assertNull(AiQuiz.checkTyped(q, "the smallest living part"))
        assertEquals(false, AiQuiz.checkTyped(q, "  "))
    }

    @Test fun numbersMatchAsNumbers() {
        val q = QuizQuestion(QuizQuestion.Kind.TYPED, "2+2.5?", emptyList(), -1, "4.5", emptyList(), "")
        assertEquals(true, AiQuiz.checkTyped(q, "4.50"))
    }

    @Test fun aReplyWithoutAQuizOrWithAnEmptyOneStaysPlainText() {
        assertEquals("hello" to null, AiQuiz.extract("hello"))
        val (text, json) = AiQuiz.extract("Sure.\n```absencium-quiz\n{\"questions\":[]}\n```")
        assertEquals("Sure.", text)
        assertNull(json)
        assertTrue(AiQuiz.parse("not json").isEmpty())
        assertFalse(AiQuiz.parse("[{\"q\":\"\"}]").isNotEmpty())
    }
}
