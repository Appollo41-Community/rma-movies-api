package rs.raf.edu.rma.users

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import rs.raf.edu.rma.users.domain.AuthResponse
import rs.raf.edu.rma.users.domain.UserDto
import kotlin.test.Test

class MeRoutingTest {

    @Test
    fun `GET me with valid token returns user`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()

        val signup = client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"full_name":"Me User","username":"me_user","password":"password123"}""")
        }.body<AuthResponse>()

        val response = client.get("/me") {
            header(HttpHeaders.Authorization, "Bearer ${signup.accessToken}")
        }

        response.status shouldBe HttpStatusCode.OK
        val user = response.body<UserDto>()
        user.username shouldBe "me_user"
        user.fullName shouldBe "Me User"
    }

    @Test
    fun `GET me without token returns 401`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()

        val response = client.get("/me")
        response.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `GET me with malformed token returns 401`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()

        val response = client.get("/me") {
            header(HttpHeaders.Authorization, "Bearer not-a-real-jwt")
        }
        response.status shouldBe HttpStatusCode.Unauthorized
    }
}
