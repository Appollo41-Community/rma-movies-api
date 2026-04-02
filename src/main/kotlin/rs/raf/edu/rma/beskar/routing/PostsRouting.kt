package rs.raf.edu.rma.beskar.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException
import org.koin.ktor.ext.inject
import rs.raf.edu.rma.beskar.domain.CommentRequest
import rs.raf.edu.rma.beskar.domain.ReactionRequest
import rs.raf.edu.rma.beskar.repository.AlreadyReactedException
import rs.raf.edu.rma.beskar.repository.BeskarRepository
import rs.raf.edu.rma.beskar.repository.ReactionNotFoundException
import rs.raf.edu.rma.core.optionalIntQueryParam
import rs.raf.edu.rma.plugins.errors.AppException

private val DATE_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")

fun Route.beskarPostsRouting(path: String) = route(path) {

    val beskarRepository by inject<BeskarRepository>()

    get {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 20
        val query = call.request.queryParameters["query"]
        val mediaType = call.request.queryParameters["media_type"]
        val categoryId = call.optionalIntQueryParam("category_id")
        val startDate = call.request.queryParameters["start_date"]
        val endDate = call.request.queryParameters["end_date"]
        val sortBy = call.request.queryParameters["sort_by"] ?: "date"
        val sortOrder = call.request.queryParameters["sort_order"] ?: "desc"

        if (page < 1) throw AppException.BadRequestException("page must be >= 1")
        if (pageSize !in 1..100) throw AppException.BadRequestException("page_size must be 1..100")
        if (mediaType != null && mediaType !in listOf("image", "video")) {
            throw AppException.BadRequestException("media_type must be 'image' or 'video'")
        }
        if (sortBy !in listOf("date", "title", "likes", "dislikes", "comments_count")) {
            throw AppException.BadRequestException("sort_by must be one of: date, title, likes, dislikes, comments_count")
        }
        if (sortOrder !in listOf("asc", "desc")) {
            throw AppException.BadRequestException("sort_order must be 'asc' or 'desc'")
        }

        val result = beskarRepository.getPosts(
            page = page,
            pageSize = pageSize,
            query = query,
            mediaType = mediaType,
            categoryId = categoryId,
            startDate = startDate,
            endDate = endDate,
            sortBy = sortBy,
            sortOrder = sortOrder,
        )
        call.respond(result)
    }

    get("apod") {
        val date = call.request.queryParameters["date"]
            ?: throw AppException.BadRequestException("date query parameter is required (format: YYYY-MM-DD)")
        if (!DATE_REGEX.matches(date)) {
            throw AppException.BadRequestException("date must be in YYYY-MM-DD format")
        }
        val post = beskarRepository.getPostByDate(date)
            ?: throw AppException.NotFoundException("No post found for date: $date")
        call.respond(post)
    }

    get("random") {
        val post = beskarRepository.getRandomPost()
            ?: throw AppException.NotFoundException("No posts available")
        call.respond(post)
    }

    get("on-this-day") {
        val month = call.optionalIntQueryParam("month")
            ?: throw AppException.BadRequestException("month query parameter is required")
        val day = call.optionalIntQueryParam("day")
            ?: throw AppException.BadRequestException("day query parameter is required")
        if (month !in 1..12) throw AppException.BadRequestException("month must be 1..12")
        if (day !in 1..31) throw AppException.BadRequestException("day must be 1..31")

        val posts = beskarRepository.getOnThisDay(month, day)
        call.respond(posts)
    }

    get("popular") {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 20
        val sortBy = call.request.queryParameters["sort_by"] ?: "likes"

        if (page < 1) throw AppException.BadRequestException("page must be >= 1")
        if (pageSize !in 1..100) throw AppException.BadRequestException("page_size must be 1..100")
        if (sortBy !in listOf("likes", "dislikes", "comments_count")) {
            throw AppException.BadRequestException("sort_by must be one of: likes, dislikes, comments_count")
        }

        val result = beskarRepository.getPopularPosts(page = page, pageSize = pageSize, sortBy = sortBy)
        call.respond(result)
    }

    get("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: throw AppException.BadRequestException("Invalid post id")
        val post = beskarRepository.getPostById(id)
            ?: throw AppException.NotFoundException("Post not found: $id")
        call.respond(post)
    }

    get("{id}/related") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: throw AppException.BadRequestException("Invalid post id")
        if (!beskarRepository.postExists(id)) {
            throw AppException.NotFoundException("Post not found: $id")
        }
        val related = beskarRepository.getRelatedPosts(id)
        call.respond(related)
    }

    // --- Reactions ---

    post("{id}/like") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: throw AppException.BadRequestException("Invalid post id")
        if (!beskarRepository.postExists(id)) {
            throw AppException.NotFoundException("Post not found: $id")
        }
        val request = try {
            call.receive<ReactionRequest>()
        } catch (e: Exception) {
            throw AppException.BadRequestException("Request body must contain 'nickname' field")
        }
        if (request.nickname.isBlank()) {
            throw AppException.BadRequestException("nickname must not be blank")
        }
        try {
            beskarRepository.likePost(id, request.nickname)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Liked"))
        } catch (e: AlreadyReactedException) {
            throw AppException.ConflictException("Already liked by ${request.nickname}")
        }
    }

    post("{id}/dislike") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: throw AppException.BadRequestException("Invalid post id")
        if (!beskarRepository.postExists(id)) {
            throw AppException.NotFoundException("Post not found: $id")
        }
        val request = try {
            call.receive<ReactionRequest>()
        } catch (e: Exception) {
            throw AppException.BadRequestException("Request body must contain 'nickname' field")
        }
        if (request.nickname.isBlank()) {
            throw AppException.BadRequestException("nickname must not be blank")
        }
        try {
            beskarRepository.dislikePost(id, request.nickname)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Disliked"))
        } catch (e: AlreadyReactedException) {
            throw AppException.ConflictException("Already disliked by ${request.nickname}")
        }
    }

    delete("{id}/like") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: throw AppException.BadRequestException("Invalid post id")
        if (!beskarRepository.postExists(id)) {
            throw AppException.NotFoundException("Post not found: $id")
        }
        val request = try {
            call.receive<ReactionRequest>()
        } catch (e: Exception) {
            throw AppException.BadRequestException("Request body must contain 'nickname' field")
        }
        try {
            beskarRepository.removeLike(id, request.nickname)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Like removed"))
        } catch (e: ReactionNotFoundException) {
            throw AppException.NotFoundException("No like found for ${request.nickname} on post $id")
        }
    }

    delete("{id}/dislike") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: throw AppException.BadRequestException("Invalid post id")
        if (!beskarRepository.postExists(id)) {
            throw AppException.NotFoundException("Post not found: $id")
        }
        val request = try {
            call.receive<ReactionRequest>()
        } catch (e: Exception) {
            throw AppException.BadRequestException("Request body must contain 'nickname' field")
        }
        try {
            beskarRepository.removeDislike(id, request.nickname)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Dislike removed"))
        } catch (e: ReactionNotFoundException) {
            throw AppException.NotFoundException("No dislike found for ${request.nickname} on post $id")
        }
    }

    // --- Comments ---

    get("{id}/comments") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: throw AppException.BadRequestException("Invalid post id")
        if (!beskarRepository.postExists(id)) {
            throw AppException.NotFoundException("Post not found: $id")
        }
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 20
        if (page < 1) throw AppException.BadRequestException("page must be >= 1")
        if (pageSize !in 1..100) throw AppException.BadRequestException("page_size must be 1..100")

        val result = beskarRepository.getComments(id, page, pageSize)
        call.respond(result)
    }

    post("{id}/comments") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: throw AppException.BadRequestException("Invalid post id")
        if (!beskarRepository.postExists(id)) {
            throw AppException.NotFoundException("Post not found: $id")
        }
        val request = try {
            call.receive<CommentRequest>()
        } catch (e: Exception) {
            throw AppException.BadRequestException("Request body must contain 'nickname' and 'text' fields")
        }
        if (request.nickname.isBlank()) {
            throw AppException.BadRequestException("nickname must not be blank")
        }
        if (request.text.isBlank()) {
            throw AppException.BadRequestException("text must not be blank")
        }
        val comment = beskarRepository.addComment(id, request.nickname, request.text)
        call.respond(HttpStatusCode.Created, comment)
    }
}
