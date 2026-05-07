package rs.raf.edu.rma.users.routing

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import rs.raf.edu.rma.plugins.errors.AppException
import rs.raf.edu.rma.users.auth.AUTH_REALM_JWT
import rs.raf.edu.rma.users.auth.requireUserId
import rs.raf.edu.rma.users.repository.AuthRepository

fun Route.meRouting() {
    val authRepository by inject<AuthRepository>()

    authenticate(AUTH_REALM_JWT) {
        get("/me") {
            val userId = call.requireUserId()
            val user = authRepository.getUser(userId)
                ?: throw AppException.UnauthorizedException("User not found")
            call.respond(user)
        }
    }
}
