package rs.raf.edu.rma.users

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import rs.raf.edu.rma.movies.domain.MovieListItem
import rs.raf.edu.rma.users.domain.AuthResponse
import kotlin.test.Test

class FavoritesRoutingTest {

    private suspend fun signup(client: io.ktor.client.HttpClient, username: String): String {
        val resp = client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"full_name":"$username","username":"$username","password":"password123"}""")
        }.body<AuthResponse>()
        return resp.accessToken
    }

    @Test
    fun `GET favorites empty for new user`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()
        val token = signup(client, "fav_empty")

        val resp = client.get("/me/favorites") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        resp.status shouldBe HttpStatusCode.OK
        resp.body<List<MovieListItem>>().shouldBeEmpty()
    }

    @Test
    fun `POST then GET favorites returns the movie`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()
        val token = signup(client, "fav_user")

        // Use a real IMDB ID known to exist in movies.db. tt0111161 = The Shawshank Redemption.
        val add = client.post("/me/favorites/tt0111161") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        add.status shouldBe HttpStatusCode.Created

        val list = client.get("/me/favorites") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        list.status shouldBe HttpStatusCode.OK
        list.body<List<MovieListItem>>().shouldHaveSize(1)
    }

    @Test
    fun `DELETE favorites removes the movie`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()
        val token = signup(client, "fav_del")

        client.post("/me/favorites/tt0111161") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val del = client.delete("/me/favorites/tt0111161") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        del.status shouldBe HttpStatusCode.NoContent

        val list = client.get("/me/favorites") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        list.body<List<MovieListItem>>().shouldBeEmpty()
    }

    @Test
    fun `POST favorites for unknown movie returns 404`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()
        val token = signup(client, "fav_404")

        val resp = client.post("/me/favorites/tt9999999_not_real") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        resp.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `POST favorites without token returns 401`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()

        val resp = client.post("/me/favorites/tt0111161")
        resp.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `POST favorites is idempotent`() = testApplication {
        val env = newTestEnv()
        installShowtimeTestEnv(env)
        val client = jsonClient()
        val token = signup(client, "fav_idemp")

        repeat(3) {
            client.post("/me/favorites/tt0111161") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        val list = client.get("/me/favorites") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        list.body<List<MovieListItem>>().shouldHaveSize(1)
    }
}
