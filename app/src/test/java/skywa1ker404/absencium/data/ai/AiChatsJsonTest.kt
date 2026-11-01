package skywa1ker404.absencium.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiChatsJsonTest {

    @Test
    fun `a chat survives being written and read again`() {
        val chat = AiChat(
            id = 7, title = "Physics", pinned = true, created = 1, updated = 2, provider = "GEMINI", model = "gemini-x", webSearch = true, study = true, thinking = true, archived = true, level = "college", assistant = "a1",
            messages = listOf(
                ChatMessage(1, ChatRole.USER, "What is torque?", 10, attachments = listOf(ChatAttachment("pic.jpg", "image/jpeg", "/data/pic.jpg"))),
                ChatMessage(2, ChatRole.ASSISTANT, "A turning force.", 11, model = "gemini-x", actions = "[{\"type\":\"add_subject\"}]", doneActions = setOf(0, 2), sources = listOf(ChatSource("Site", "https://a.example/x"))),
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

class AiMemoryTest {
    @org.junit.Test fun memoriesRoundTripThroughJson() {
        val items = listOf(MemoryItem(1, "Studies BSc Physics", null, 1, 2), MemoryItem(2, "Prefers short answers here", 9L, 3, 3))
        org.junit.Assert.assertEquals(items, AiMemoryJson.decode(AiMemoryJson.encode(items)))
    }

    @org.junit.Test fun aShortListIsSentWholeAndALongOneIsRankedByOverlap() {
        val few = listOf(MemoryItem(1, "likes tea"), MemoryItem(2, "studies physics", 5L))
        org.junit.Assert.assertEquals(2, AiMemory.pick(few, "anything", 5L, 10).size)
        val many = (1..30L).map { MemoryItem(it, if (it == 7L) "exam in thermodynamics" else "fact number $it") }
        val picked = AiMemory.pick(many, "help with thermodynamic exams", null, 5)
        org.junit.Assert.assertEquals(5, picked.size)
        org.junit.Assert.assertEquals(7L, picked.first().id)
    }
}

class AiAssistantsTest {
    @org.junit.Test fun anExportedFileReadsBackAndBadEntriesAreSkipped() {
        val items = listOf(AssistantProfile("a", "Maths", "Be rigorous.", "GEMINI", "gemini-x", 0.4))
        org.junit.Assert.assertEquals(items, AiAssistantsJson.decode(AiAssistantsJson.encode(items)))
        val mixed = """{"format":"absencium-assistants","assistants":[{"name":"","instructions":"x"},{"name":"Ok","instructions":"Do it","temperature":9}]}"""
        val read = AiAssistantsJson.decode(mixed)
        org.junit.Assert.assertEquals(1, read.size)
        org.junit.Assert.assertEquals(2.0, read[0].temperature!!, 0.0)
    }
}
