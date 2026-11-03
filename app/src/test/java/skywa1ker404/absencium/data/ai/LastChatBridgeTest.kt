package skywa1ker404.absencium.data.ai

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessagePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LastChatBridgeTest {
    private val bridge = LastChatBridge()

    @Test
    fun systemFirstThenTurnsInOrder() {
        val out = bridge.messagesOf("rules", listOf(ChatTurn(ChatRole.USER, "hi"), ChatTurn(ChatRole.ASSISTANT, "hello")))
        assertEquals(listOf(MessageRole.SYSTEM, MessageRole.USER, MessageRole.ASSISTANT), out.map { it.role })
        assertEquals("rules", out[0].toText())
        assertEquals("hello", out[2].toText())
    }

    @Test
    fun filesBecomeTheRightParts() {
        val turn = ChatTurn(
            ChatRole.USER, "look",
            listOf(AiImage("image/png", "AAA"), AiImage("application/pdf", "BBB"), AiImage("audio/mp3", "CCC")),
        )
        val parts = bridge.messagesOf("s", listOf(turn))[1].parts
        assertTrue(parts[0] is UIMessagePart.Image)
        assertTrue(parts[1] is UIMessagePart.Document)
        assertTrue(parts[2] is UIMessagePart.Audio)
        assertEquals("look", (parts[3] as UIMessagePart.Text).text)
        assertEquals("data:image/png;base64,AAA", (parts[0] as UIMessagePart.Image).url)
    }

    @Test
    fun httpFailuresKeepTheirCode() {
        val limit = bridge.failureOf(Exception("Failed to get response: 429 {\"error\":{\"message\":\"Quota exceeded\"}}"))
        assertEquals(429, limit.httpCode)
        assertTrue(limit.limitReached)
        assertTrue(limit.message.orEmpty().contains("Quota exceeded"))
        val busy = bridge.failureOf(Exception("Failed to get response: 503 overloaded"))
        assertTrue(busy.temporary)
        assertFalse(bridge.failureOf(Exception("Failed to get response: 401 bad key")).limitReached)
    }

    @Test
    fun otherFailuresAreReadable() {
        assertTrue(bridge.failureOf(java.io.IOException("x")).message.orEmpty().startsWith("Could not reach"))
        assertEquals("boom", bridge.failureOf(IllegalStateException("boom")).message)
    }
}
