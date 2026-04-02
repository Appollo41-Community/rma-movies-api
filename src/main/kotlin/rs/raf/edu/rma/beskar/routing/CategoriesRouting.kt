package rs.raf.edu.rma.beskar.routing

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import rs.raf.edu.rma.beskar.repository.BeskarRepository
import rs.raf.edu.rma.plugins.errors.AppException

fun Route.beskarCategoriesRouting(path: String) = route(path) {

    val beskarRepository by inject<BeskarRepository>()

    get {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 20
        if (page < 1) throw AppException.BadRequestException("page must be >= 1")
        if (pageSize !in 1..100) throw AppException.BadRequestException("page_size must be 1..100")

        val categories = beskarRepository.getCategories(page = page, pageSize = pageSize)
        call.respond(categories)
    }

    get("popular") {
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        if (limit !in 1..100) throw AppException.BadRequestException("limit must be 1..100")

        val categories = beskarRepository.getPopularCategories(limit = limit)
        call.respond(categories)
    }
}

fun Route.beskarStatsRouting(path: String) = route(path) {

    val beskarRepository by inject<BeskarRepository>()

    get {
        val stats = beskarRepository.getStats()
        call.respond(stats)
    }
}
