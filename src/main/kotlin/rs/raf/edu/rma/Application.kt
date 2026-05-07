package rs.raf.edu.rma

import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import rs.raf.edu.rma.beskar.di.beskar
import rs.raf.edu.rma.movies.di.movies
import rs.raf.edu.rma.plugins.errors.configureErrorStatusPages
import rs.raf.edu.rma.plugins.monitoring.configureMonitoring
import rs.raf.edu.rma.plugins.routing.configureRouting
import rs.raf.edu.rma.plugins.serialization.configureSerialization
import org.koin.ktor.ext.get
import rs.raf.edu.rma.users.auth.JwtConfig
import rs.raf.edu.rma.users.auth.configureAuth
import rs.raf.edu.rma.users.di.users

fun main(args: Array<String>) = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused")
fun Application.coreModule() {
    install(Koin) {
        slf4jLogger()
        modules(
            movies,
            beskar,
            users,
        )
    }

    val jwtConfig = get<JwtConfig>()
    configureAuth(jwtConfig)

    configureMonitoring()
    configureSerialization()
    configureErrorStatusPages()
    configureRouting()
}
