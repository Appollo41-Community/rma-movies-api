package rs.raf.edu.rma.users.repository

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import rs.raf.edu.rma.plugins.errors.AppException
import rs.raf.edu.rma.users.auth.BcryptPasswordHasher
import rs.raf.edu.rma.users.auth.JwtConfig
import rs.raf.edu.rma.users.domain.AuthResponse
import rs.raf.edu.rma.users.domain.LoginRequest
import rs.raf.edu.rma.users.domain.SignupRequest
import rs.raf.edu.rma.users.domain.UserDto
import rs.raf.edu.rma.users.domain.UsersTable

class AuthRepository(
    private val database: Database,
    private val passwordHasher: BcryptPasswordHasher,
    private val jwtConfig: JwtConfig,
) {

    fun signup(request: SignupRequest): AuthResponse {
        validateUsername(request.username)
        validatePassword(request.password)
        validateFullName(request.fullName)

        val now = System.currentTimeMillis()
        val hashed = passwordHasher.hash(request.password)

        val userId = transaction(database) {
            val existing = UsersTable.select(UsersTable.username eq request.username).singleOrNull()
            if (existing != null) {
                throw AppException.ConflictException(
                    message = "Username '${request.username}' is already taken",
                )
            }
            UsersTable.insert {
                it[username] = request.username
                it[fullName] = request.fullName
                it[passwordHash] = hashed
                it[createdAt] = now
            } get UsersTable.id
        }

        val user = UserDto(id = userId, username = request.username, fullName = request.fullName)
        val token = jwtConfig.sign(userId = userId, username = request.username)
        return AuthResponse(accessToken = token, expiresIn = jwtConfig.expiresInSeconds, user = user)
    }

    fun login(request: LoginRequest): AuthResponse {
        val row = transaction(database) {
            UsersTable.select(UsersTable.username eq request.username).singleOrNull()
        } ?: throw AppException.UnauthorizedException("Invalid username or password")

        val storedHash = row[UsersTable.passwordHash]
        if (!passwordHasher.verify(request.password, storedHash)) {
            throw AppException.UnauthorizedException("Invalid username or password")
        }

        val user = row.toUserDto()
        val token = jwtConfig.sign(userId = user.id, username = user.username)
        return AuthResponse(accessToken = token, expiresIn = jwtConfig.expiresInSeconds, user = user)
    }

    fun getUser(userId: Int): UserDto? = transaction(database) {
        UsersTable.select(UsersTable.id eq userId).singleOrNull()?.toUserDto()
    }

    private fun ResultRow.toUserDto() = UserDto(
        id = this[UsersTable.id],
        username = this[UsersTable.username],
        fullName = this[UsersTable.fullName],
    )

    private fun validateUsername(username: String) {
        if (!USERNAME_REGEX.matches(username)) {
            throw AppException.BadRequestException(
                message = "Username must be at least 3 characters and contain only letters, digits, and underscores",
            )
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw AppException.BadRequestException(
                message = "Password must be at least 8 characters",
            )
        }
    }

    private fun validateFullName(fullName: String) {
        if (fullName.isBlank()) {
            throw AppException.BadRequestException(message = "Full name must not be blank")
        }
    }

    companion object {
        private val USERNAME_REGEX = Regex("^[A-Za-z0-9_]{3,}$")
    }
}
