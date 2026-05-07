package rs.raf.edu.rma.users

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import rs.raf.edu.rma.movies.domain.PaginatedResponse
import rs.raf.edu.rma.users.domain.AuthResponse
import rs.raf.edu.rma.users.domain.LeaderboardEntry
import rs.raf.edu.rma.users.domain.PostQuizResultResponse
import rs.raf.edu.rma.users.domain.QuizResultDto
import kotlin.test.Test

class LeaderboardRoutingTest {

    private suspend fun signup(client: io.ktor.client.HttpClient, username: String): String {
        return client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"full_name":"$username","username":"$username","password":"password123"}""")
        }.body<AuthResponse>().accessToken
    }

    @Test
    fun `POST leaderboard returns ranking 1 for first entry`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()
        val token = signup(client, "lb_solo")

        val resp = client.post("/leaderboard") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"score":80.5,"category":1}""")
        }
        resp.status shouldBe HttpStatusCode.OK
        val body = resp.body<PostQuizResultResponse>()
        body.ranking shouldBe 1
        body.result.score shouldBe 80.5f
        body.result.category shouldBe 1
    }

    @Test
    fun `GET leaderboard sorts score DESC then played_at ASC`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()

        val tokenA = signup(client, "user_a")
        val tokenB = signup(client, "user_b")
        val tokenC = signup(client, "user_c")

        // A scores 90 first, B scores 90 second (tiebreaker: A ranks higher), C scores 80.
        client.post("/leaderboard") {
            header(HttpHeaders.Authorization, "Bearer $tokenA")
            contentType(ContentType.Application.Json)
            setBody("""{"score":90.0,"category":1}""")
        }
        Thread.sleep(2)
        client.post("/leaderboard") {
            header(HttpHeaders.Authorization, "Bearer $tokenB")
            contentType(ContentType.Application.Json)
            setBody("""{"score":90.0,"category":1}""")
        }
        Thread.sleep(2)
        client.post("/leaderboard") {
            header(HttpHeaders.Authorization, "Bearer $tokenC")
            contentType(ContentType.Application.Json)
            setBody("""{"score":80.0,"category":1}""")
        }

        val resp = client.get("/leaderboard?category=1&page=1&page_size=20")
        resp.status shouldBe HttpStatusCode.OK
        val board = resp.body<PaginatedResponse<LeaderboardEntry>>()
        board.items.shouldHaveSize(3)
        board.items[0].username shouldBe "user_a"
        board.items[0].rank shouldBe 1
        board.items[1].username shouldBe "user_b"
        board.items[1].rank shouldBe 2
        board.items[2].username shouldBe "user_c"
        board.items[2].rank shouldBe 3
    }

    @Test
    fun `POST leaderboard rejects out-of-range score with 400`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()
        val token = signup(client, "bad_score")

        val tooHigh = client.post("/leaderboard") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"score":120.0,"category":1}""")
        }
        tooHigh.status shouldBe HttpStatusCode.BadRequest

        val negative = client.post("/leaderboard") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"score":-1.0,"category":1}""")
        }
        negative.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `POST leaderboard without token returns 401`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()

        val resp = client.post("/leaderboard") {
            contentType(ContentType.Application.Json)
            setBody("""{"score":50.0,"category":1}""")
        }
        resp.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `GET me quiz-results returns own history sorted desc`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()
        val token = signup(client, "history_user")

        client.post("/leaderboard") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"score":50.0,"category":1}""")
        }
        Thread.sleep(2)
        client.post("/leaderboard") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"score":70.0,"category":1}""")
        }

        val resp = client.get("/me/quiz-results?page=1&page_size=20") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        resp.status shouldBe HttpStatusCode.OK
        val results = resp.body<PaginatedResponse<QuizResultDto>>()
        results.items.shouldHaveSize(2)
        // Most recent first.
        results.items[0].score shouldBe 70.0f
        results.items[1].score shouldBe 50.0f
    }
}
