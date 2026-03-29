package rs.raf.edu.rma.plugins.errors

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.SerializationException
import rs.raf.edu.rma.plugins.errors.model.AppExceptionResponse

fun Application.configureErrorStatusPages() {
    val log = log

    install(StatusPages) {
        exception<AppException> { call, appException ->
            log.error("AppException: ${appException.error}", appException)
            call.respond(
                status = appException.httpStatusCode,
                message = AppExceptionResponse(
                    error = appException.error,
                    httpCode = appException.httpStatusCode.value,
                    message = appException.message,
                    description = appException.description,
                    suggestion = appException.suggestion,
                )
            )
        }

        exception<RequestValidationException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = AppExceptionResponse(
                    error = HttpStatusCode.BadRequest.description,
                    httpCode = HttpStatusCode.BadRequest.value,
                    message = cause.reasons.joinToString()
                )
            )
        }

        exception<SerializationException> { call, cause ->
            log.error("SerializationException", cause)
            call.respond(
                status = HttpStatusCode.UnprocessableEntity,
                message = AppExceptionResponse(
                    error = HttpStatusCode.UnprocessableEntity.description,
                    httpCode = HttpStatusCode.UnprocessableEntity.value,
                    message = "Invalid request body.",
                )
            )
        }

        exception<Exception> { call, cause ->
            log.error("Unhandled exception", cause)
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = AppExceptionResponse(
                    error = "unknown",
                    httpCode = HttpStatusCode.InternalServerError.value,
                    message = "An internal server error occurred.",
                )
            )
        }
    }

}
