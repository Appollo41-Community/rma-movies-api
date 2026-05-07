package rs.raf.edu.rma.users.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import rs.raf.edu.rma.plugins.errors.AppException
import rs.raf.edu.rma.users.auth.AUTH_REALM_JWT
import rs.raf.edu.rma.users.auth.requireUserId
import rs.raf.edu.rma.users.repository.AuthRepository
import rs.raf.edu.rma.users.repository.FavoritesRepository

fun Route.meRouting() {
    val authRepository by inject<AuthRepository>()
    val favoritesRepository by inject<FavoritesRepository>()

    authenticate(AUTH_REALM_JWT) {
        get("/me") {
            val userId = call.requireUserId()
            val user = authRepository.getUser(userId)
                ?: throw AppException.UnauthorizedException("User not found")
            call.respond(user)
        }

        route("/me/favorites") {
            get {
                val userId = call.requireUserId()
                call.respond(favoritesRepository.list(userId))
            }
            post("{movieId}") {
                val userId = call.requireUserId()
                val movieId = call.parameters["movieId"]
                    ?: throw AppException.BadRequestException("Missing movieId")
                val added = favoritesRepository.add(userId, movieId)
                call.respond(if (added) HttpStatusCode.Created else HttpStatusCode.OK)
            }
            delete("{movieId}") {
                val userId = call.requireUserId()
                val movieId = call.parameters["movieId"]
                    ?: throw AppException.BadRequestException("Missing movieId")
                favoritesRepository.remove(userId, movieId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
