package rs.raf.edu.rma.users

import io.kotest.matchers.shouldBe
import rs.raf.edu.rma.users.auth.BcryptPasswordHasher
import kotlin.test.Test

class BcryptPasswordHasherTest {
    private val hasher = BcryptPasswordHasher(cost = 4)

    @Test
    fun `hash produces a non-empty string distinct from the plain password`() {
        val hash = hasher.hash("hunter2!!")
        (hash.isNotBlank()) shouldBe true
        (hash == "hunter2!!") shouldBe false
    }

    @Test
    fun `verify returns true for the original password`() {
        val hash = hasher.hash("hunter2!!")
        hasher.verify("hunter2!!", hash) shouldBe true
    }

    @Test
    fun `verify returns false for a different password`() {
        val hash = hasher.hash("hunter2!!")
        hasher.verify("wrong", hash) shouldBe false
    }
}
