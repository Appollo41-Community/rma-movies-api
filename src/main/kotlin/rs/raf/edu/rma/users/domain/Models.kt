package rs.raf.edu.rma.users.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    @SerialName("full_name") val fullName: String,
    val username: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class UserDto(
    val id: Int,
    val username: String,
    @SerialName("full_name") val fullName: String,
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    val user: UserDto,
)

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    @SerialName("user_id") val userId: Int,
    val username: String,
    @SerialName("full_name") val fullName: String,
    val score: Float,
    @SerialName("played_at") val playedAt: Long,
    @SerialName("total_plays") val totalPlays: Int,
)

@Serializable
data class QuizResultDto(
    val id: Int,
    val category: Int,
    val score: Float,
    @SerialName("played_at") val playedAt: Long,
)

@Serializable
data class PostQuizResultRequest(
    val score: Float,
    val category: Int,
)

@Serializable
data class PostQuizResultResponse(
    val result: QuizResultDto,
    val ranking: Int,
)
