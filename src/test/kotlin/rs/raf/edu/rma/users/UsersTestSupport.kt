package rs.raf.edu.rma.users

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import java.io.File

class TestEnv(
    val usersDbFile: File,
    val moviesDbFile: File,
)

fun newTestEnv(): TestEnv {
    val usersDb = File.createTempFile("showtime-users-", ".db").apply { deleteOnExit() }
    val moviesDb = File("data/movies.db")
    check(moviesDb.exists()) { "data/movies.db not found — required for cross-DB validation tests" }
    return TestEnv(usersDb, moviesDb)
}

fun ApplicationTestBuilder.installShowtimeTestEnv(env: TestEnv) {
    System.setProperty("USERS_DATABASE_URL", env.usersDbFile.absolutePath)
    System.setProperty("DATABASE_URL", env.moviesDbFile.absolutePath)
    System.setProperty("JWT_SECRET", "test-secret-do-not-use-in-prod")
    environment {
        config = ApplicationConfig("application.conf")
    }
}

fun ApplicationTestBuilder.jsonClient(): HttpClient = createClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
}
