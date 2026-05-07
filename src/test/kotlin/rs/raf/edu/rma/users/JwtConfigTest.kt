package rs.raf.edu.rma.users

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import rs.raf.edu.rma.users.auth.JwtConfig
import kotlin.test.Test
import kotlin.test.assertFailsWith

class JwtConfigTest {

    private val cfg = JwtConfig(secret = "test-secret-please-change-in-prod", expiresInSeconds = 60L)

    @Test
    fun `sign produces a non-empty token containing three dot-separated segments`() {
        val token = cfg.sign(userId = 42, username = "rma_test")
        token.isNotBlank() shouldBe true
        token.split(".").size shouldBe 3
    }

    @Test
    fun `verifier accepts tokens we just signed and exposes claims`() {
        val token = cfg.sign(userId = 42, username = "rma_test")
        val decoded = cfg.verifier.verify(token)
        decoded.subject shouldBe "42"
        decoded.getClaim("username").asString() shouldBe "rma_test"
    }

    @Test
    fun `verifier rejects tokens signed with a different secret`() {
        val other = JwtConfig(secret = "other-secret", expiresInSeconds = 60L)
        val token = other.sign(userId = 1, username = "x")
        assertFailsWith<Exception> { cfg.verifier.verify(token) }
    }

    @Test
    fun `constructor rejects empty secret`() {
        assertFailsWith<IllegalArgumentException> { JwtConfig(secret = "") }
    }
}
