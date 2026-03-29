package rs.raf.edu.rma.movies.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import rs.raf.edu.rma.core.optionalFloatQueryParam
import rs.raf.edu.rma.core.optionalIntQueryParam
import rs.raf.edu.rma.movies.repository.MoviesRepository
import rs.raf.edu.rma.plugins.errors.AppException

fun Route.moviesRouting(path: String) = route(path) {

    val moviesRepository by inject<MoviesRepository>()

    get {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 20
        val genreId = call.optionalIntQueryParam("genre_id")
        val query = call.request.queryParameters["query"]
        val sortBy = call.request.queryParameters["sort_by"] ?: "imdb_votes"
        val sortOrder = call.request.queryParameters["sort_order"] ?: "desc"
        val minYear = call.optionalIntQueryParam("min_year")
        val maxYear = call.optionalIntQueryParam("max_year")
        val minRating = call.optionalFloatQueryParam("min_rating")

        if (page < 1) throw AppException.BadRequestException("page must be >= 1")
        if (pageSize !in 1..100) throw AppException.BadRequestException("page_size must be 1..100")
        if (minRating != null && (minRating !in 0f..10f))
            throw AppException.BadRequestException("min_rating must be between 0.0 and 10.0")
        if (minYear != null && maxYear != null && minYear > maxYear)
            throw AppException.BadRequestException("min_year must be less than or equal to max_year")

        val result = moviesRepository.getMovies(
            page = page,
            pageSize = pageSize,
            genreId = genreId,
            query = query,
            sortBy = sortBy,
            sortOrder = sortOrder,
            minYear = minYear,
            maxYear = maxYear,
            minRating = minRating,
        )
        call.respond(result)
    }

    get("{id}") {
        val id = call.parameters["id"]
            ?: throw AppException.BadRequestException("Missing movie id")
        val movie = moviesRepository.getMovieById(id)
            ?: throw AppException.NotFoundException("Movie not found: $id")
        call.respond(movie)
    }

    get("{id}/cast") {
        val id = call.parameters["id"]
            ?: throw AppException.BadRequestException("Missing movie id")
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 20
        if (page < 1) throw AppException.BadRequestException("page must be >= 1")
        if (pageSize !in 1..100) throw AppException.BadRequestException("page_size must be 1..100")
        val cast = moviesRepository.getCastForMovie(id, page, pageSize)
        call.respond(cast)
    }

    get("{id}/images") {
        val id = call.parameters["id"]
            ?: throw AppException.BadRequestException("Missing movie id")
        val type = call.request.queryParameters["type"]
        val images = moviesRepository.getImagesForMovie(id, type)
        call.respond(images)
    }

    get("{id}/videos") {
        val id = call.parameters["id"]
            ?: throw AppException.BadRequestException("Missing movie id")
        val type = call.request.queryParameters["type"]
        val videos = moviesRepository.getVideosForMovie(id, type)
        call.respond(videos)
    }

    get("{id}/companies") {
        val id = call.parameters["id"]
            ?: throw AppException.BadRequestException("Missing movie id")
        val companies = moviesRepository.getCompaniesForMovie(id)
        call.respond(companies)
    }
}

fun Route.peopleRouting(path: String) = route(path) {

    val moviesRepository by inject<MoviesRepository>()

    get("{id}") {
        val id = call.parameters["id"]
            ?: throw AppException.BadRequestException("Missing person id")
        val personDetail = moviesRepository.getPersonById(id)
            ?: throw AppException.NotFoundException("Person not found: $id")
        call.respond(personDetail)
    }
}

fun Route.genresRouting(path: String) = route(path) {

    val moviesRepository by inject<MoviesRepository>()

    get {
        val genres = moviesRepository.getGenres()
        call.respond(genres)
    }
}

fun Route.collectionsRouting(path: String) = route(path) {

    val moviesRepository by inject<MoviesRepository>()

    get("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: throw AppException.BadRequestException("Missing or invalid collection id")
        val result = moviesRepository.getCollectionById(id)
            ?: throw AppException.NotFoundException("Collection not found: $id")
        call.respond(
            rs.raf.edu.rma.movies.domain.CollectionDetail(
                collection = result.first,
                movies = result.second,
            )
        )
    }
}

fun Route.configRouting(path: String) = route(path) {

    val moviesRepository by inject<MoviesRepository>()

    get {
        val config = moviesRepository.getConfig()
        call.respond(config)
    }
}
