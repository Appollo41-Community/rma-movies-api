package rs.raf.edu.rma.users.di

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.qualifier.named
import org.koin.dsl.module
import rs.raf.edu.rma.users.auth.BcryptPasswordHasher
import rs.raf.edu.rma.users.auth.JwtConfig
import rs.raf.edu.rma.users.domain.FavoritesTable
import rs.raf.edu.rma.users.domain.QuizResultsTable
import rs.raf.edu.rma.users.domain.UsersTable
import rs.raf.edu.rma.users.domain.WatchlistTable
import rs.raf.edu.rma.users.repository.AuthRepository
import rs.raf.edu.rma.users.repository.FavoritesRepository
import rs.raf.edu.rma.users.repository.LeaderboardRepository
import rs.raf.edu.rma.users.repository.WatchlistRepository

val users = module {

    single(named("usersDb")) {
        val dbUrl = System.getProperty("USERS_DATABASE_URL")
            ?: System.getenv("USERS_DATABASE_URL")
            ?: "data/users.db"
        val db = Database.connect("jdbc:sqlite:$dbUrl", driver = "org.sqlite.JDBC")
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                FavoritesTable,
                WatchlistTable,
                QuizResultsTable,
            )
        }
        db
    }

    single { BcryptPasswordHasher() }

    single {
        val secret = System.getProperty("JWT_SECRET")
            ?: System.getenv("JWT_SECRET")
            ?: error("JWT_SECRET environment variable not set — refusing to start with a default secret")
        JwtConfig(secret = secret)
    }

    single {
        AuthRepository(
            database = get(named("usersDb")),
            passwordHasher = get(),
            jwtConfig = get(),
        )
    }
    single {
        FavoritesRepository(
            usersDatabase = get(named("usersDb")),
            moviesDatabase = get(),
        )
    }
    single {
        WatchlistRepository(
            usersDatabase = get(named("usersDb")),
            moviesDatabase = get(),
        )
    }
    single {
        LeaderboardRepository(database = get(named("usersDb")))
    }
}
