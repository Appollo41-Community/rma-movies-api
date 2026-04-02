package rs.raf.edu.rma.plugins.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import rs.raf.edu.rma.core.readReleaseVersion
import rs.raf.edu.rma.beskar.routing.*
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

        route("beskar") {
            beskarPostsRouting(path = "posts")
            beskarCollectionsRouting(path = "collections")
            beskarCategoriesRouting(path = "categories")
            beskarStatsRouting(path = "stats")
        }

        get("/docs") {
            val html = this::class.java.classLoader.getResource("static/docs-index.html")?.readText()
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respondText(html, ContentType.Text.Html)
        }

        get("/movies/docs") {
            val html = this::class.java.classLoader.getResource("static/docs.html")?.readText()
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respondText(html, ContentType.Text.Html)
        }

        get("/beskar/docs") {
            val html = this::class.java.classLoader.getResource("static/beskar/docs.html")?.readText()
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respondText(html, ContentType.Text.Html)
        }

        get("/premiere/demo") {
            val html = this::class.java.classLoader.getResource("static/premiere/demo.html")?.readText()
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respondText(html, ContentType.Text.Html)
        }

        get("/premiere/spec") {
            val html = this::class.java.classLoader.getResource("static/premiere/spec.html")?.readText()
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respondText(html, ContentType.Text.Html)
        }
    }
}
