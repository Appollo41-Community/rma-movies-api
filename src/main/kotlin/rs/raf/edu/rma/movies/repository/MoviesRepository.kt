package rs.raf.edu.rma.movies.repository

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import rs.raf.edu.rma.movies.domain.*

class MoviesRepository(private val database: Database) {

    fun getMovies(
        page: Int = 1,
        pageSize: Int = 20,
        genreId: Int? = null,
        query: String? = null,
        sortBy: String = "imdb_votes",
        sortOrder: String = "desc",
        minYear: Int? = null,
        maxYear: Int? = null,
        minRating: Float? = null,
    ): PaginatedResponse<MovieListItem> = transaction(database) {
        val baseQuery = if (genreId != null) {
            MoviesTable.innerJoin(MovieGenresTable, { MoviesTable.imdbId }, { MovieGenresTable.movieId })
                .slice(MoviesTable.columns)
                .selectAll()
                .andWhere { MovieGenresTable.genreId eq genreId }
        } else {
            MoviesTable.selectAll()
        }

        if (!query.isNullOrBlank()) {
            val sanitized = query.replace("%", "\\%").replace("_", "\\_")
            baseQuery.andWhere { MoviesTable.title like "%${sanitized}%" }
        }

        if (minYear != null) {
            baseQuery.andWhere { MoviesTable.year greaterEq minYear }
        }
        if (maxYear != null) {
            baseQuery.andWhere { MoviesTable.year lessEq maxYear }
        }
        if (minRating != null) {
            baseQuery.andWhere { MoviesTable.imdbRating greaterEq minRating }
        }

        val totalItems = baseQuery.count().toInt()
        val totalPages = if (totalItems == 0) 0 else (totalItems + pageSize - 1) / pageSize

        val sortColumn: Column<*> = when (sortBy) {
            "year" -> MoviesTable.year
            "imdb_rating" -> MoviesTable.imdbRating
            "tmdb_rating" -> MoviesTable.tmdbRating
            "popularity" -> MoviesTable.popularity
            "title" -> MoviesTable.title
            else -> MoviesTable.imdbVotes
        }

        val order = if (sortOrder.equals("asc", ignoreCase = true)) SortOrder.ASC else SortOrder.DESC

        val movieRows = baseQuery.copy()
            .orderBy(sortColumn to order)
            .limit(pageSize, offset = (page.toLong() - 1) * pageSize)
            .toList()

        val movieIds = movieRows.map { it[MoviesTable.imdbId] }
        val genresByMovie = getGenresForMovies(movieIds)

        val movies = movieRows.map { row ->
                val imdbId = row[MoviesTable.imdbId]
                MovieListItem(
                    imdbId = imdbId,
                    title = row[MoviesTable.title],
                    year = row[MoviesTable.year],
                    imdbRating = row[MoviesTable.imdbRating],
                    imdbVotes = row[MoviesTable.imdbVotes],
                    posterPath = row[MoviesTable.posterPath],
                    genres = genresByMovie[imdbId] ?: emptyList(),
                )
            }

        PaginatedResponse(
            page = page,
            pageSize = pageSize,
            totalItems = totalItems,
            totalPages = totalPages,
            items = movies,
        )
    }

    fun getMovieById(imdbId: String): Movie? = transaction(database) {
        val row = MoviesTable.select(MoviesTable.imdbId eq imdbId)
            .singleOrNull() ?: return@transaction null

        val genres = getGenresForMovie(imdbId)
        val collection = row[MoviesTable.collectionId]?.let { getCollection(it) }

        Movie(
            imdbId = row[MoviesTable.imdbId],
            tmdbId = row[MoviesTable.tmdbId],
            title = row[MoviesTable.title],
            originalTitle = row[MoviesTable.originalTitle],
            overview = row[MoviesTable.overview],
            tagline = row[MoviesTable.tagline],
            releaseDate = row[MoviesTable.releaseDate],
            year = row[MoviesTable.year],
            runtime = row[MoviesTable.runtime],
            budget = row[MoviesTable.budget],
            revenue = row[MoviesTable.revenue],
            languageCode = row[MoviesTable.languageCode],
            popularity = row[MoviesTable.popularity],
            imdbRating = row[MoviesTable.imdbRating],
            imdbVotes = row[MoviesTable.imdbVotes],
            tmdbRating = row[MoviesTable.tmdbRating],
            tmdbVotes = row[MoviesTable.tmdbVotes],
            posterPath = row[MoviesTable.posterPath],
            backdropPath = row[MoviesTable.backdropPath],
            homepage = row[MoviesTable.homepage],
            genres = genres,
            collection = collection,
        )
    }

    fun getPersonById(imdbId: String): PersonDetail? = transaction(database) {
        val row = PeopleTable.select(PeopleTable.imdbId eq imdbId)
            .singleOrNull() ?: return@transaction null

        val department = row[PeopleTable.departmentId]?.let { deptId ->
            DepartmentsTable.select(DepartmentsTable.id eq deptId)
                .singleOrNull()?.get(DepartmentsTable.name)
        }

        val person = Person(
            imdbId = row[PeopleTable.imdbId],
            tmdbId = row[PeopleTable.tmdbId],
            name = row[PeopleTable.name],
            birthYear = row[PeopleTable.birthYear],
            deathYear = row[PeopleTable.deathYear],
            professions = row[PeopleTable.professions],
            department = department,
            popularity = row[PeopleTable.popularity],
            profilePath = row[PeopleTable.profilePath],
            gender = row[PeopleTable.gender],
        )

        val movieRows = MoviePeopleTable.innerJoin(MoviesTable, { MoviePeopleTable.movieId }, { MoviesTable.imdbId })
            .select(MoviePeopleTable.personId eq imdbId)
            .orderBy(MoviesTable.imdbVotes to SortOrder.DESC)
            .toList()

        val movieIds = movieRows.map { it[MoviesTable.imdbId] }
        val genresByMovie = getGenresForMovies(movieIds)

        val movies = movieRows.map { mRow ->
                val movieImdbId = mRow[MoviesTable.imdbId]
                MovieListItem(
                    imdbId = movieImdbId,
                    title = mRow[MoviesTable.title],
                    year = mRow[MoviesTable.year],
                    imdbRating = mRow[MoviesTable.imdbRating],
                    imdbVotes = mRow[MoviesTable.imdbVotes],
                    posterPath = mRow[MoviesTable.posterPath],
                    genres = genresByMovie[movieImdbId] ?: emptyList(),
                )
            }

        PersonDetail(person = person, movies = movies)
    }

    fun getGenres(): List<Genre> = transaction(database) {
        GenresTable.selectAll()
            .orderBy(GenresTable.name to SortOrder.ASC)
            .map { Genre(id = it[GenresTable.id], name = it[GenresTable.name]) }
    }

    fun getCollectionById(id: Int): Pair<MovieCollection, List<MovieListItem>>? = transaction(database) {
        val row = CollectionsTable.select(CollectionsTable.id eq id)
            .singleOrNull() ?: return@transaction null

        val collection = MovieCollection(
            id = row[CollectionsTable.id],
            name = row[CollectionsTable.name],
            posterPath = row[CollectionsTable.posterPath],
            backdropPath = row[CollectionsTable.backdropPath],
        )

        val movieRows = MoviesTable.select(MoviesTable.collectionId eq id)
            .orderBy(MoviesTable.year to SortOrder.ASC)
            .toList()

        val movieIds = movieRows.map { it[MoviesTable.imdbId] }
        val genresByMovie = getGenresForMovies(movieIds)

        val movies = movieRows.map { mRow ->
                val movieImdbId = mRow[MoviesTable.imdbId]
                MovieListItem(
                    imdbId = movieImdbId,
                    title = mRow[MoviesTable.title],
                    year = mRow[MoviesTable.year],
                    imdbRating = mRow[MoviesTable.imdbRating],
                    imdbVotes = mRow[MoviesTable.imdbVotes],
                    posterPath = mRow[MoviesTable.posterPath],
                    genres = genresByMovie[movieImdbId] ?: emptyList(),
                )
            }

        Pair(collection, movies)
    }

    fun getConfig(): List<ConfigEntry> = transaction(database) {
        ConfigTable.selectAll().map {
            ConfigEntry(key = it[ConfigTable.key], value = it[ConfigTable.value])
        }
    }

    private fun getGenresForMovie(imdbId: String): List<Genre> {
        return MovieGenresTable.innerJoin(GenresTable, { MovieGenresTable.genreId }, { GenresTable.id })
            .select(MovieGenresTable.movieId eq imdbId)
            .map { Genre(id = it[GenresTable.id], name = it[GenresTable.name]) }
    }

    private fun getGenresForMovies(imdbIds: List<String>): Map<String, List<Genre>> {
        if (imdbIds.isEmpty()) return emptyMap()
        return MovieGenresTable.innerJoin(GenresTable, { MovieGenresTable.genreId }, { GenresTable.id })
            .select { MovieGenresTable.movieId inList imdbIds }
            .groupBy(
                keySelector = { it[MovieGenresTable.movieId] },
                valueTransform = { Genre(id = it[GenresTable.id], name = it[GenresTable.name]) },
            )
    }

    fun getCastForMovie(
        imdbId: String,
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<PersonSummary> = transaction(database) {
        val baseQuery = MoviePeopleTable.innerJoin(PeopleTable, { MoviePeopleTable.personId }, { PeopleTable.imdbId })
            .select(MoviePeopleTable.movieId eq imdbId)

        val totalItems = baseQuery.count().toInt()
        val totalPages = if (totalItems == 0) 0 else (totalItems + pageSize - 1) / pageSize

        val items = MoviePeopleTable
            .innerJoin(PeopleTable, { MoviePeopleTable.personId }, { PeopleTable.imdbId })
            .leftJoin(DepartmentsTable, { PeopleTable.departmentId }, { DepartmentsTable.id })
            .select(MoviePeopleTable.movieId eq imdbId)
            .orderBy(PeopleTable.popularity to SortOrder.DESC_NULLS_LAST)
            .limit(pageSize, offset = (page.toLong() - 1) * pageSize)
            .map { row ->
                PersonSummary(
                    imdbId = row[PeopleTable.imdbId],
                    name = row[PeopleTable.name],
                    professions = row[PeopleTable.professions],
                    department = row.getOrNull(DepartmentsTable.name),
                    profilePath = row[PeopleTable.profilePath],
                )
            }

        PaginatedResponse(
            page = page,
            pageSize = pageSize,
            totalItems = totalItems,
            totalPages = totalPages,
            items = items,
        )
    }

    fun getImagesForMovie(imdbId: String, type: String? = null): MovieImages = transaction(database) {
        val query = MovieImagesTable.innerJoin(ImageTypesTable, { MovieImagesTable.typeId }, { ImageTypesTable.id })
            .select(MovieImagesTable.movieId eq imdbId)
            .orderBy(MovieImagesTable.voteAverage to SortOrder.DESC_NULLS_LAST)

        if (type != null) {
            query.andWhere { ImageTypesTable.name eq type }
        }

        val allImages = query.map { row ->
            val typeName = row[ImageTypesTable.name]
            typeName to MovieImage(
                filePath = row[MovieImagesTable.filePath],
                width = row[MovieImagesTable.width],
                height = row[MovieImagesTable.height],
                voteAverage = row[MovieImagesTable.voteAverage],
                language = row[MovieImagesTable.language],
            )
        }

        MovieImages(
            posters = allImages.filter { it.first == "poster" }.map { it.second },
            backdrops = allImages.filter { it.first == "backdrop" }.map { it.second },
            logos = allImages.filter { it.first == "logo" }.map { it.second },
        )
    }

    fun getVideosForMovie(imdbId: String, type: String? = null): List<Video> = transaction(database) {
        val query = MovieVideosTable
            .innerJoin(VideoSitesTable, { MovieVideosTable.siteId }, { VideoSitesTable.id })
            .innerJoin(VideoTypesTable, { MovieVideosTable.typeId }, { VideoTypesTable.id })
            .select(MovieVideosTable.movieId eq imdbId)

        if (type != null) {
            query.andWhere { VideoTypesTable.name eq type }
        }

        query.map { row ->
            Video(
                key = row[MovieVideosTable.key],
                site = row[VideoSitesTable.name],
                name = row[MovieVideosTable.name],
                type = row[VideoTypesTable.name],
                official = row[MovieVideosTable.official] == 1,
                publishedAt = row[MovieVideosTable.publishedAt],
            )
        }
    }

    fun getCompaniesForMovie(imdbId: String): List<ProductionCompany> = transaction(database) {
        MovieCompaniesTable.innerJoin(ProductionCompaniesTable, { MovieCompaniesTable.companyId }, { ProductionCompaniesTable.id })
            .select(MovieCompaniesTable.movieId eq imdbId)
            .map { row ->
                ProductionCompany(
                    id = row[ProductionCompaniesTable.id],
                    name = row[ProductionCompaniesTable.name],
                    logoPath = row[ProductionCompaniesTable.logoPath],
                    originCountry = row[ProductionCompaniesTable.originCountry],
                )
            }
    }

    private fun getCollection(id: Int): MovieCollection? {
        return CollectionsTable.select(CollectionsTable.id eq id)
            .singleOrNull()?.let { row ->
                MovieCollection(
                    id = row[CollectionsTable.id],
                    name = row[CollectionsTable.name],
                    posterPath = row[CollectionsTable.posterPath],
                    backdropPath = row[CollectionsTable.backdropPath],
                )
            }
    }
}
