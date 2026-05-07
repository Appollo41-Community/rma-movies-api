package rs.raf.edu.rma.users.repository

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import rs.raf.edu.rma.movies.domain.Genre
import rs.raf.edu.rma.movies.domain.GenresTable
import rs.raf.edu.rma.movies.domain.MovieGenresTable
import rs.raf.edu.rma.movies.domain.MovieListItem
import rs.raf.edu.rma.movies.domain.MoviesTable
import rs.raf.edu.rma.plugins.errors.AppException
import rs.raf.edu.rma.users.domain.WatchlistTable

class WatchlistRepository(
    private val usersDatabase: Database,
    private val moviesDatabase: Database,
) {

    fun list(userId: Int): List<MovieListItem> {
        val movieIds = transaction(usersDatabase) {
            WatchlistTable.select(WatchlistTable.userId eq userId)
                .orderBy(WatchlistTable.createdAt to SortOrder.DESC)
                .map { it[WatchlistTable.movieId] }
        }
        if (movieIds.isEmpty()) return emptyList()
        return loadMovieListItems(movieIds)
    }

    fun add(userId: Int, movieId: String): Boolean {
        ensureMovieExists(movieId)
        return transaction(usersDatabase) {
            val existing = WatchlistTable.select(
                (WatchlistTable.userId eq userId) and (WatchlistTable.movieId eq movieId)
            ).singleOrNull()
            if (existing != null) {
                false
            } else {
                WatchlistTable.insertIgnore {
                    it[WatchlistTable.userId] = userId
                    it[WatchlistTable.movieId] = movieId
                    it[WatchlistTable.createdAt] = System.currentTimeMillis()
                }
                true
            }
        }
    }

    fun remove(userId: Int, movieId: String) {
        transaction(usersDatabase) {
            WatchlistTable.deleteWhere {
                (WatchlistTable.userId eq userId) and (WatchlistTable.movieId eq movieId)
            }
        }
    }

    private fun ensureMovieExists(movieId: String) {
        val exists = transaction(moviesDatabase) {
            MoviesTable.select(MoviesTable.imdbId eq movieId).any()
        }
        if (!exists) throw AppException.NotFoundException("Movie not found: $movieId")
    }

    private fun loadMovieListItems(movieIds: List<String>): List<MovieListItem> = transaction(moviesDatabase) {
        val movieRows = MoviesTable.select { MoviesTable.imdbId inList movieIds }.toList()
        val genresByMovie = MovieGenresTable
            .innerJoin(GenresTable, { MovieGenresTable.genreId }, { GenresTable.id })
            .select { MovieGenresTable.movieId inList movieIds }
            .groupBy(
                keySelector = { it[MovieGenresTable.movieId] },
                valueTransform = { Genre(id = it[GenresTable.id], name = it[GenresTable.name]) },
            )
        val byId = movieRows.associateBy { it[MoviesTable.imdbId] }
        movieIds.mapNotNull { id ->
            val row = byId[id] ?: return@mapNotNull null
            MovieListItem(
                imdbId = id,
                title = row[MoviesTable.title],
                year = row[MoviesTable.year],
                imdbRating = row[MoviesTable.imdbRating],
                imdbVotes = row[MoviesTable.imdbVotes],
                posterPath = row[MoviesTable.posterPath],
                genres = genresByMovie[id] ?: emptyList(),
            )
        }
    }
}
