package rs.raf.edu.rma.users.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import rs.raf.edu.rma.users.domain.LoginRequest
import rs.raf.edu.rma.users.domain.SignupRequest
import rs.raf.edu.rma.users.repository.AuthRepository

fun Route.authRouting(path: String) = route(path) {

    val authRepository by inject<AuthRepository>()

    post("signup") {
        val request = call.receive<SignupRequest>()
        val response = authRepository.signup(request)
        call.respond(HttpStatusCode.Created, response)
    }

    post("login") {
        val request = call.receive<LoginRequest>()
        val response = authRepository.login(request)
        call.respond(HttpStatusCode.OK, response)
    }
}
