package rs.raf.edu.rma.users

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import rs.raf.edu.rma.movies.domain.MovieListItem
import rs.raf.edu.rma.users.domain.AuthResponse
import kotlin.test.Test

class WatchlistRoutingTest {

    private suspend fun signup(client: io.ktor.client.HttpClient, username: String): String {
        return client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"full_name":"$username","username":"$username","password":"password123"}""")
        }.body<AuthResponse>().accessToken
    }

    @Test
    fun `add to watchlist and list it back`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()
        val token = signup(client, "wl_user")

        val add = client.post("/me/watchlist/tt0111161") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        add.status shouldBe HttpStatusCode.Created

        val list = client.get("/me/watchlist") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        list.body<List<MovieListItem>>().shouldHaveSize(1)
    }

    @Test
    fun `remove from watchlist`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()
        val token = signup(client, "wl_del")

        client.post("/me/watchlist/tt0111161") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val del = client.delete("/me/watchlist/tt0111161") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        del.status shouldBe HttpStatusCode.NoContent
    }

    @Test
    fun `favorites and watchlist are independent lists`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()
        val token = signup(client, "wl_indep")

        client.post("/me/favorites/tt0111161") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        val watchlist = client.get("/me/watchlist") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        watchlist.body<List<MovieListItem>>().shouldHaveSize(0)
    }
}
