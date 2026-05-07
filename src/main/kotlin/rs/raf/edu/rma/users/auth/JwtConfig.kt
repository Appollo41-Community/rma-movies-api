package rs.raf.edu.rma.users.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class JwtConfig(
    private val secret: String,
    val expiresInSeconds: Long = DEFAULT_EXPIRES_IN_SECONDS,
) {
    init {
        require(secret.isNotBlank()) { "JWT secret must not be blank" }
    }

    private val algorithm: Algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm).build()

    fun sign(userId: Int, username: String): String {
        val nowMs = System.currentTimeMillis()
        return JWT.create()
            .withSubject(userId.toString())
            .withClaim("username", username)
            .withIssuedAt(Date(nowMs))
            .withExpiresAt(Date(nowMs + expiresInSeconds * 1000))
            .sign(algorithm)
    }

    companion object {
        const val DEFAULT_EXPIRES_IN_SECONDS: Long = 60L * 60L * 24L * 60L // 60 days
    }
}
