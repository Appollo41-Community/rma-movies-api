package rs.raf.edu.rma.users.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import rs.raf.edu.rma.plugins.errors.AppException

fun ApplicationCall.requireUserId(): Int {
    val principal = principal<JWTPrincipal>()
        ?: throw AppException.UnauthorizedException("Missing JWT principal")
    return principal.payload.subject?.toIntOrNull()
        ?: throw AppException.UnauthorizedException("Malformed JWT subject")
}
