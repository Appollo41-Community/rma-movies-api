package rs.raf.edu.rma.plugins.routing

import io.ktor.http.*
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

        get("/docs") {
            val html = this::class.java.classLoader.getResource("static/docs.html")?.readText()
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
