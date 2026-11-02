package skywa1ker404.absencium.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiOrchestratorTest {
    @Test fun aGreetingIsSimpleAndNeedsNothing() {
        val plan = AiOrchestrator.plan("hello", "hello", AiMode.BALANCED)
        assertEquals(Complexity.SIMPLE, plan.complexity)
        assertFalse(plan.topics.data)
        assertFalse(plan.topics.actions)
    }

    @Test fun hardOrLongWorkIsComplexAndNeverCapped() {
        val plan = AiOrchestrator.plan("derive the formula step by step", "derive the formula step by step", AiMode.EFFICIENT)
        assertEquals(Complexity.COMPLEX, plan.complexity)
        assertNull(plan.budget.maxOutput)
        assertEquals(Complexity.COMPLEX, AiOrchestrator.plan("x", "make revision notes on optics", AiMode.EFFICIENT).complexity)
    }

    @Test fun efficientCapsShortAnswersAndBalancedDoesNot() {
        assertEquals(800, AiOrchestrator.plan("what is a vein", "what is a vein", AiMode.EFFICIENT).budget.maxOutput)
        assertNull(AiOrchestrator.plan("what is a vein", "what is a vein", AiMode.BALANCED).budget.maxOutput)
    }

    @Test fun modesScaleHistoryAndMemories() {
        val topics = PromptTopics()
        val e = AiOrchestrator.budget(AiMode.EFFICIENT, Complexity.NORMAL, topics)
        val b = AiOrchestrator.budget(AiMode.BALANCED, Complexity.NORMAL, topics)
        val m = AiOrchestrator.budget(AiMode.MAX, Complexity.NORMAL, topics)
        assertTrue(e.fullTurns < b.fullTurns && b.fullTurns < m.fullTurns)
        assertTrue(e.memories < b.memories && b.memories < m.memories)
    }
}
