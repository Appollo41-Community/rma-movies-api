package rs.raf.edu.rma.movies.di

import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module
import rs.raf.edu.rma.movies.repository.MoviesRepository

val movies = module {
    single {
        val dbUrl = System.getenv("DATABASE_URL") ?: "data/movies.db"
        Database.connect("jdbc:sqlite:$dbUrl", driver = "org.sqlite.JDBC")
    }

    single {
        MoviesRepository(database = get())
    }
}
