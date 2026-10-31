package skywa1ker404.absencium.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiChatsJsonTest {

    @Test
    fun `a chat survives being written and read again`() {
        val chat = AiChat(
            id = 7, title = "Physics", pinned = true, created = 1, updated = 2, provider = "GEMINI", model = "gemini-x",
            messages = listOf(
                ChatMessage(1, ChatRole.USER, "What is torque?", 10),
                ChatMessage(2, ChatRole.ASSISTANT, "A turning force.", 11, model = "gemini-x", actions = "[{\"type\":\"add_subject\"}]", doneActions = setOf(0, 2)),
                ChatMessage(3, ChatRole.ASSISTANT, "", 12, failed = true),
            ),
        )
        val back = AiChatsJson.decode(AiChatsJson.encode(listOf(chat))).single()
        assertEquals(chat, back)
    }

    @Test
    fun `optional fields stay null and an unknown role does not crash`() {
        val back = AiChatsJson.decode("""[{"id":1,"messages":[{"id":1,"role":"ROBOT","text":"hi","time":3}]}]""").single()
        assertNull(back.provider)
        assertNull(back.model)
        assertEquals(ChatRole.USER, back.messages.single().role)
        assertTrue(back.messages.single().doneActions.isEmpty())
    }

    @Test
    fun `the label falls back to the first line of the first question`() {
        val chat = AiChat(1, "", false, 0, 0, messages = listOf(ChatMessage(1, ChatRole.USER, "Explain entropy\nin detail", 0)))
        assertEquals("Explain entropy", chat.label)
        assertEquals("New chat", AiChat(2, "", false, 0, 0).label)
        assertEquals("Named", chat.copy(title = "Named").label)
    }
}
