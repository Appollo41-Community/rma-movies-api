package rs.raf.edu.rma.plugins.errors

import io.ktor.http.*

sealed class AppException(
    val error: String = "undefined",
    val httpStatusCode: HttpStatusCode,
    message: String? = null,
    val description: String? = null,
    val suggestion: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    // 400
    open class BadRequestException(
        message: String? = null,
        cause: Throwable? = null,
        description: String? = null,
        suggestion: String? = null,
    ) : AppException(
        httpStatusCode = HttpStatusCode.BadRequest,
        message = message,
        cause = cause,
        description = description,
        suggestion = suggestion,
    )

    // 401
    open class UnauthorizedException(message: String? = null, cause: Throwable? = null) :
        AppException(message = message, cause = cause, httpStatusCode = HttpStatusCode.Unauthorized)

    // 403
    open class ForbiddenException(message: String? = null, cause: Throwable? = null) :
        AppException(message = message, cause = cause, httpStatusCode = HttpStatusCode.Forbidden)

    // 404
    open class NotFoundException(
        message: String? = null,
        cause: Throwable? = null,
        description: String? = null,
        suggestion: String? = null,
    ) : AppException(
        httpStatusCode = HttpStatusCode.NotFound,
        message = message,
        cause = cause,
        description = description,
        suggestion = suggestion,
    )

    //409
    open class ConflictException(
        message: String? = null,
        description: String? = null,
        suggestion: String? = null,
        cause: Throwable? = null
    ) : AppException(
        message = message,
        description = description,
        suggestion = suggestion,
        cause = cause,
        httpStatusCode = HttpStatusCode.Conflict
    )

    //500
    open class InternalServerErrorException(message: String? = null, cause: Throwable? = null) :
        AppException(message = message, cause = cause, httpStatusCode = HttpStatusCode.InternalServerError)

    // 501
    open class NotImplementedException(message: String? = null, cause: Throwable? = null) :
        AppException(message = message, cause = cause, httpStatusCode = HttpStatusCode.NotImplemented)
}
