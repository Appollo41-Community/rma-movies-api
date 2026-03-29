package rs.raf.edu.rma.plugins.routing

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import rs.raf.edu.rma.core.readReleaseVersion
import rs.raf.edu.rma.movies.routing.*

fun Application.configureRouting() {

    val appVersion = readReleaseVersion()

    routing {
        get("/") {
            call.respondText(
                """
                    RMA Movies API $appVersion.
                """.trimIndent()
            )
        }

        moviesRouting(path = "movies")
        peopleRouting(path = "people")
        genresRouting(path = "genres")
        collectionsRouting(path = "collections")
        configRouting(path = "config")

        staticResources("/demo", "static") {
            default("demo.html")
        }

        staticResources("/docs", "static") {
            default("docs.html")
        }
    }
}
