package rs.raf.edu.rma.movies.domain

import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    val imdbId: String,
    val tmdbId: Int? = null,
    val title: String,
    val originalTitle: String? = null,
    val overview: String? = null,
    val tagline: String? = null,
    val releaseDate: String? = null,
    val year: Int? = null,
    val runtime: Int? = null,
    val budget: Long? = null,
    val revenue: Long? = null,
    val languageCode: String? = null,
    val popularity: Float? = null,
    val imdbRating: Float? = null,
    val imdbVotes: Int? = null,
    val tmdbRating: Float? = null,
    val tmdbVotes: Int? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val homepage: String? = null,
    val genres: List<Genre> = emptyList(),
    val collection: MovieCollection? = null,
)

@Serializable
data class MovieListItem(
    val imdbId: String,
    val title: String,
    val year: Int? = null,
    val imdbRating: Float? = null,
    val imdbVotes: Int? = null,
    val posterPath: String? = null,
    val genres: List<Genre> = emptyList(),
)

@Serializable
data class CollectionDetail(
    val collection: MovieCollection,
    val movies: List<MovieListItem> = emptyList(),
)

@Serializable
data class Genre(
    val id: Int,
    val name: String,
)

@Serializable
data class Person(
    val imdbId: String,
    val tmdbId: Int? = null,
    val name: String,
    val birthYear: Int? = null,
    val deathYear: Int? = null,
    val professions: String? = null,
    val department: String? = null,
    val popularity: Float? = null,
    val profilePath: String? = null,
    val gender: Int? = null,
)

@Serializable
data class PersonSummary(
    val imdbId: String,
    val name: String,
    val professions: String? = null,
    val department: String? = null,
    val profilePath: String? = null,
)

@Serializable
data class PersonDetail(
    val person: Person,
    val movies: List<MovieListItem> = emptyList(),
)

@Serializable
data class MovieImage(
    val filePath: String,
    val width: Int? = null,
    val height: Int? = null,
    val voteAverage: Float? = null,
    val language: String? = null,
)

@Serializable
data class MovieImages(
    val posters: List<MovieImage> = emptyList(),
    val backdrops: List<MovieImage> = emptyList(),
    val logos: List<MovieImage> = emptyList(),
)

@Serializable
data class Video(
    val key: String,
    val site: String,
    val name: String? = null,
    val type: String? = null,
    val official: Boolean = false,
    val publishedAt: String? = null,
)

@Serializable
data class MovieCollection(
    val id: Int,
    val name: String,
    val posterPath: String? = null,
    val backdropPath: String? = null,
)

@Serializable
data class ProductionCompany(
    val id: Int,
    val name: String,
    val logoPath: String? = null,
    val originCountry: String? = null,
)

@Serializable
data class ConfigEntry(
    val key: String,
    val value: String,
)

@Serializable
data class PaginatedResponse<T>(
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val items: List<T>,
)
