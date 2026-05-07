package rs.raf.edu.rma.users.domain

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = integer("id").autoIncrement()
    val username = text("username").uniqueIndex("idx_users_username")
    val fullName = text("full_name")
    val passwordHash = text("password_hash")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object FavoritesTable : Table("favorites") {
    val userId = integer("user_id").references(UsersTable.id)
    val movieId = text("movie_id")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(userId, movieId)

    init {
        index(false, userId)
    }
}

object WatchlistTable : Table("watchlist") {
    val userId = integer("user_id").references(UsersTable.id)
    val movieId = text("movie_id")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(userId, movieId)

    init {
        index(false, userId)
    }
}

object QuizResultsTable : Table("quiz_results") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(UsersTable.id)
    val category = integer("category")
    val score = float("score")
    val playedAt = long("played_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, category, score)
        index(false, userId)
    }
}
