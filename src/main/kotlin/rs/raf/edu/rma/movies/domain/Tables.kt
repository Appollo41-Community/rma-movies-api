package rs.raf.edu.rma.movies.domain

import org.jetbrains.exposed.sql.Table

object MoviesTable : Table("movies") {
    val imdbId = text("imdb_id")
    val tmdbId = integer("tmdb_id").nullable()
    val title = text("title")
    val originalTitle = text("original_title").nullable()
    val overview = text("overview").nullable()
    val tagline = text("tagline").nullable()
    val releaseDate = text("release_date").nullable()
    val year = integer("year").nullable()
    val runtime = integer("runtime").nullable()
    val budget = long("budget").nullable()
    val revenue = long("revenue").nullable()
    val languageCode = text("language_code").nullable()
    val popularity = float("popularity").nullable()
    val imdbRating = float("imdb_rating").nullable()
    val imdbVotes = integer("imdb_votes").nullable()
    val tmdbRating = float("tmdb_rating").nullable()
    val tmdbVotes = integer("tmdb_votes").nullable()
    val posterPath = text("poster_path").nullable()
    val backdropPath = text("backdrop_path").nullable()
    val homepage = text("homepage").nullable()
    val collectionId = integer("collection_id").nullable()

    override val primaryKey = PrimaryKey(imdbId)
}

object PeopleTable : Table("people") {
    val imdbId = text("imdb_id")
    val tmdbId = integer("tmdb_id").nullable()
    val name = text("name")
    val birthYear = integer("birth_year").nullable()
    val deathYear = integer("death_year").nullable()
    val professions = text("professions").nullable()
    val departmentId = integer("department_id").nullable()
    val popularity = float("popularity").nullable()
    val profilePath = text("profile_path").nullable()
    val gender = integer("gender").nullable()

    override val primaryKey = PrimaryKey(imdbId)
}

object GenresTable : Table("genres") {
    val id = integer("id")
    val name = text("name")

    override val primaryKey = PrimaryKey(id)
}

object MovieGenresTable : Table("movie_genres") {
    val movieId = text("movie_id")
    val genreId = integer("genre_id")

    override val primaryKey = PrimaryKey(movieId, genreId)
}

object MovieImagesTable : Table("movie_images") {
    val id = integer("id").autoIncrement()
    val movieId = text("movie_id")
    val typeId = integer("type_id")
    val filePath = text("file_path")
    val width = integer("width").nullable()
    val height = integer("height").nullable()
    val voteAverage = float("vote_average").nullable()
    val language = text("language").nullable()

    override val primaryKey = PrimaryKey(id)
}

object MovieVideosTable : Table("movie_videos") {
    val id = text("id")
    val movieId = text("movie_id")
    val key = text("key")
    val siteId = integer("site_id")
    val name = text("name").nullable()
    val typeId = integer("type_id")
    val official = integer("official").nullable()
    val publishedAt = text("published_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object MoviePeopleTable : Table("movie_people") {
    val movieId = text("movie_id")
    val personId = text("person_id")

    override val primaryKey = PrimaryKey(movieId, personId)
}

object CollectionsTable : Table("collections") {
    val id = integer("id")
    val name = text("name")
    val posterPath = text("poster_path").nullable()
    val backdropPath = text("backdrop_path").nullable()

    override val primaryKey = PrimaryKey(id)
}

object ProductionCompaniesTable : Table("production_companies") {
    val id = integer("id")
    val name = text("name")
    val logoPath = text("logo_path").nullable()
    val originCountry = text("origin_country").nullable()

    override val primaryKey = PrimaryKey(id)
}

object MovieCompaniesTable : Table("movie_companies") {
    val movieId = text("movie_id")
    val companyId = integer("company_id")

    override val primaryKey = PrimaryKey(movieId, companyId)
}

object VideoTypesTable : Table("video_types") {
    val id = integer("id").autoIncrement()
    val name = text("name")

    override val primaryKey = PrimaryKey(id)
}

object VideoSitesTable : Table("video_sites") {
    val id = integer("id").autoIncrement()
    val name = text("name")

    override val primaryKey = PrimaryKey(id)
}

object ImageTypesTable : Table("image_types") {
    val id = integer("id").autoIncrement()
    val name = text("name")

    override val primaryKey = PrimaryKey(id)
}

object DepartmentsTable : Table("departments") {
    val id = integer("id").autoIncrement()
    val name = text("name")

    override val primaryKey = PrimaryKey(id)
}

object LanguagesTable : Table("languages") {
    val code = text("code")
    val name = text("name").nullable()

    override val primaryKey = PrimaryKey(code)
}

object ConfigTable : Table("config") {
    val key = text("key")
    val value = text("value")

    override val primaryKey = PrimaryKey(key)
}
