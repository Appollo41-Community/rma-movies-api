package rs.raf.edu.rma.movies.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import rs.raf.edu.rma.movies.repository.MoviesRepository
import rs.raf.edu.rma.plugins.errors.AppException

fun Route.moviesRouting(path: String) = route(path) {

    val moviesRepository by inject<MoviesRepository>()

    get {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 20
        val genreId = call.request.queryParameters["genre_id"]?.toIntOrNull()
        val query = call.request.queryParameters["query"]
        val sortBy = call.request.queryParameters["sort_by"] ?: "imdb_votes"
        val sortOrder = call.request.queryParameters["sort_order"] ?: "desc"

        if (page < 1) throw AppException.BadRequestException("page must be >= 1")
        if (pageSize < 1 || pageSize > 100) throw AppException.BadRequestException("page_size must be 1..100")

        val result = moviesRepository.getMovies(
            page = page,
            pageSize = pageSize,
            genreId = genreId,
            query = query,
            sortBy = sortBy,
            sortOrder = sortOrder,
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
        if (pageSize < 1 || pageSize > 100) throw AppException.BadRequestException("page_size must be 1..100")
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
