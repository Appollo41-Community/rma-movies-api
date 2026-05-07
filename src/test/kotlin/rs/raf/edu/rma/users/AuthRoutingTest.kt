package rs.raf.edu.rma.users

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.*
import rs.raf.edu.rma.users.domain.AuthResponse
import kotlin.test.Test

class AuthRoutingTest {

    @Test
    fun `signup happy path returns 201 with token and user`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()

        val response = client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"full_name":"Test User","username":"test_user","password":"password123"}""")
        }

        response.status shouldBe HttpStatusCode.Created
        val body = response.body<AuthResponse>()
        body.accessToken.shouldNotBeEmpty()
        body.user.username shouldBe "test_user"
        body.user.fullName shouldBe "Test User"
    }

    @Test
    fun `signup with taken username returns 409`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()

        client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"full_name":"A","username":"dup_user","password":"password123"}""")
        }
        val second = client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"full_name":"B","username":"dup_user","password":"password123"}""")
        }

        second.status shouldBe HttpStatusCode.Conflict
    }

    @Test
    fun `signup with short password returns 400`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()

        val response = client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"full_name":"X","username":"shorty","password":"abc"}""")
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `login with valid credentials returns token`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()

        client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"full_name":"Login User","username":"login_user","password":"password123"}""")
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"login_user","password":"password123"}""")
        }

        response.status shouldBe HttpStatusCode.OK
        response.body<AuthResponse>().user.username shouldBe "login_user"
    }

    @Test
    fun `login with wrong password returns 401`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()

        client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"full_name":"X","username":"wrongpass","password":"password123"}""")
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"wrongpass","password":"WRONG_password"}""")
        }

        response.status shouldBe HttpStatusCode.Unauthorized
    }
}
