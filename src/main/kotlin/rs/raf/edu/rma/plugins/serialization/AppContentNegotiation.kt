package rs.raf.edu.rma.plugins.serialization

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

val AppJson = Json {
    prettyPrint = true
}

fun Application.configureSerialization() {

    install(ContentNegotiation) {
        json(AppJson)
    }

}
