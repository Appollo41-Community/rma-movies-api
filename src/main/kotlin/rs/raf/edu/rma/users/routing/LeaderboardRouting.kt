package rs.raf.edu.rma.users.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import rs.raf.edu.rma.core.optionalIntQueryParam
import rs.raf.edu.rma.plugins.errors.AppException
import rs.raf.edu.rma.users.auth.AUTH_REALM_JWT
import rs.raf.edu.rma.users.auth.requireUserId
import rs.raf.edu.rma.users.domain.PostQuizResultRequest
import rs.raf.edu.rma.users.domain.PostQuizResultResponse
import rs.raf.edu.rma.users.repository.LeaderboardRepository

fun Route.leaderboardRouting(path: String) = route(path) {

    val leaderboardRepository by inject<LeaderboardRepository>()

    get {
        val category = call.optionalIntQueryParam("category") ?: 1
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 20
        if (page < 1) throw AppException.BadRequestException("page must be >= 1")
        if (pageSize !in 1..100) throw AppException.BadRequestException("page_size must be 1..100")
        if (category != 1) throw AppException.BadRequestException("Only category=1 is supported")
        val result = leaderboardRepository.paginate(category = category, page = page, pageSize = pageSize)
        call.respond(result)
    }

    authenticate(AUTH_REALM_JWT) {
        post {
            val userId = call.requireUserId()
            val request = call.receive<PostQuizResultRequest>()
            if (request.score !in 0.0f..100.0f) {
                throw AppException.BadRequestException("score must be in [0.00, 100.00]")
            }
            if (request.category != 1) {
                throw AppException.BadRequestException("Only category=1 is supported")
            }
            val (result, ranking) = leaderboardRepository.addResult(userId, request.category, request.score)
            call.respond(HttpStatusCode.OK, PostQuizResultResponse(result = result, ranking = ranking))
        }
    }
}
