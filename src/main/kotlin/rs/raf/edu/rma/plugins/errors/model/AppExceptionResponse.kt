package rs.raf.edu.rma.plugins.errors.model

import kotlinx.serialization.Serializable

@Serializable
data class AppExceptionResponse(
    val error: String,
    val httpCode: Int,
    val message: String? = null,
    val description: String? = null,
    val suggestion: String? = null,
    val cause: String? = null,
)