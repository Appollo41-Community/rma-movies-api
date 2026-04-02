package rs.raf.edu.rma.beskar.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import rs.raf.edu.rma.beskar.domain.CreateCollectionRequest
import rs.raf.edu.rma.beskar.domain.PostCollection
import rs.raf.edu.rma.beskar.domain.UpdateCollectionRequest
import rs.raf.edu.rma.beskar.repository.BeskarRepository
import rs.raf.edu.rma.plugins.errors.AppException

fun Route.beskarCollectionsRouting(path: String) = route(path) {

    val beskarRepository by inject<BeskarRepository>()

    post {
        val request = try {
            call.receive<CreateCollectionRequest>()
        } catch (e: Exception) {
            throw AppException.BadRequestException("Request body must contain 'nickname', 'name', and 'postIds' fields")
        }
        if (request.nickname.isBlank()) {
            throw AppException.BadRequestException("nickname must not be blank")
        }
        if (request.name.isBlank()) {
            throw AppException.BadRequestException("name must not be blank")
        }
        if (request.postIds.isNotEmpty()) {
            val invalid = beskarRepository.validatePostIds(request.postIds)
            if (invalid.isNotEmpty()) {
                throw AppException.BadRequestException("Post IDs not found: ${invalid.joinToString(", ")}")
            }
        }
        val collection: PostCollection = beskarRepository.createCollection(
            nickname = request.nickname,
            name = request.name,
            postIds = request.postIds,
        )
        call.respond(HttpStatusCode.Created, collection)
    }

    get("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: throw AppException.BadRequestException("Invalid collection id")
        val collection: PostCollection = beskarRepository.getCollectionById(id)
            ?: throw AppException.NotFoundException("Collection not found: $id")
        call.respond(collection)
    }

    put("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: throw AppException.BadRequestException("Invalid collection id")
        if (!beskarRepository.collectionExists(id)) {
            throw AppException.NotFoundException("Collection not found: $id")
        }
        val request = try {
            call.receive<UpdateCollectionRequest>()
        } catch (e: Exception) {
            throw AppException.BadRequestException("Request body must contain 'name' and 'postIds' fields")
        }
        if (request.name.isBlank()) {
            throw AppException.BadRequestException("name must not be blank")
        }
        if (request.postIds.isNotEmpty()) {
            val invalid = beskarRepository.validatePostIds(request.postIds)
            if (invalid.isNotEmpty()) {
                throw AppException.BadRequestException("Post IDs not found: ${invalid.joinToString(", ")}")
            }
        }
        val collection: PostCollection = beskarRepository.updateCollection(
            id = id,
            name = request.name,
            postIds = request.postIds,
        )
        call.respond(collection)
    }
}
