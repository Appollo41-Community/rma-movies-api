package rs.raf.edu.rma.users.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import rs.raf.edu.rma.plugins.errors.AppException

const val AUTH_REALM_JWT = "auth-jwt"

fun Application.configureAuth(jwtConfig: JwtConfig) {
    install(Authentication) {
        jwt(AUTH_REALM_JWT) {
            verifier(jwtConfig.verifier)
            validate { credential ->
                val sub = credential.payload.subject?.toIntOrNull()
                if (sub != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                throw AppException.UnauthorizedException("Invalid or missing token")
            }
        }
    }
}
