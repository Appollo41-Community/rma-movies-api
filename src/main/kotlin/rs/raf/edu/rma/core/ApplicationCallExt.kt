package rs.raf.edu.rma.core

import io.ktor.server.application.*
import rs.raf.edu.rma.plugins.errors.AppException

fun ApplicationCall.intParamOrThrow(paramName: String): Int {
    return try {
        stringParamOrThrow(paramName = paramName).toInt()
    } catch (error: NumberFormatException) {
        throw AppException.BadRequestException(
            message = "$paramName must be type of Int",
            description = "You have called the endpoint with invalid `$paramName` query parameter.",
            suggestion = "Please make sure that '$paramName' query parameter is of type Int."
        )
    }
}

fun ApplicationCall.optionalIntQueryParam(name: String): Int? {
    val raw = request.queryParameters[name] ?: return null
    return raw.toIntOrNull()
        ?: throw AppException.BadRequestException("$name must be a valid integer")
}

fun ApplicationCall.optionalFloatQueryParam(name: String): Float? {
    val raw = request.queryParameters[name] ?: return null
    return raw.toFloatOrNull()
        ?: throw AppException.BadRequestException("$name must be a valid number")
}

fun ApplicationCall.stringParamOrThrow(paramName: String): String {
    val paramValue = parameters[paramName]

    if (paramValue.isNullOrEmpty()) {
        throw AppException.BadRequestException(
            message = "$paramName must not be null or empty",
            description = "You have called the endpoint without or with empty `$paramName` query parameter.",
            suggestion = "Please add '$paramName' query parameter to your request."
        )
    }

    return paramValue
}
