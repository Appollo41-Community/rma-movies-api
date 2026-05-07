package rs.raf.edu.rma.users.repository

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import rs.raf.edu.rma.movies.domain.PaginatedResponse
import rs.raf.edu.rma.users.domain.LeaderboardEntry
import rs.raf.edu.rma.users.domain.QuizResultDto
import rs.raf.edu.rma.users.domain.QuizResultsTable
import rs.raf.edu.rma.users.domain.UsersTable

class LeaderboardRepository(private val database: Database) {

    fun paginate(category: Int, page: Int, pageSize: Int): PaginatedResponse<LeaderboardEntry> = transaction(database) {
        val baseFilter = QuizResultsTable.category eq category
        val totalItems = QuizResultsTable.select(baseFilter).count().toInt()
        val totalPages = if (totalItems == 0) 0 else (totalItems + pageSize - 1) / pageSize
        if (totalItems == 0) {
            return@transaction PaginatedResponse(
                page = page, pageSize = pageSize, totalItems = 0, totalPages = 0, items = emptyList(),
            )
        }

        // Sort: score DESC, played_at ASC (earlier achievement breaks ties).
        val rows = QuizResultsTable.innerJoin(UsersTable, { userId }, { id })
            .select(baseFilter)
            .orderBy(
                QuizResultsTable.score to SortOrder.DESC,
                QuizResultsTable.playedAt to SortOrder.ASC,
            )
            .limit(pageSize, offset = (page.toLong() - 1) * pageSize)
            .toList()

        // Compute total_plays per user for the rows on this page (one query).
        val userIds = rows.map { it[QuizResultsTable.userId] }.distinct()
        val playsByUser: Map<Int, Int> = if (userIds.isEmpty()) emptyMap() else {
            QuizResultsTable
                .select { (QuizResultsTable.category eq category) and (QuizResultsTable.userId inList userIds) }
                .groupBy { it[QuizResultsTable.userId] }
                .mapValues { (_, list) -> list.size }
        }

        val baseRank = (page - 1) * pageSize
        val items = rows.mapIndexed { idx, row ->
            LeaderboardEntry(
                rank = baseRank + idx + 1,
                userId = row[UsersTable.id],
                username = row[UsersTable.username],
                fullName = row[UsersTable.fullName],
                score = row[QuizResultsTable.score],
                playedAt = row[QuizResultsTable.playedAt],
                totalPlays = playsByUser[row[UsersTable.id]] ?: 0,
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

    /**
     * Inserts a new quiz result and returns the inserted id together with the result's
     * global ranking (its 1-based position in the category leaderboard right now).
     */
    fun addResult(userId: Int, category: Int, score: Float): Pair<QuizResultDto, Int> = transaction(database) {
        val now = System.currentTimeMillis()
        val insertedId: Int = QuizResultsTable.insert {
            it[QuizResultsTable.userId] = userId
            it[QuizResultsTable.category] = category
            it[QuizResultsTable.score] = score
            it[QuizResultsTable.playedAt] = now
        } get QuizResultsTable.id

        // Ranking: count of rows in same category that beat this one on (score DESC, played_at ASC), + 1.
        val betterCount = QuizResultsTable
            .select {
                (QuizResultsTable.category eq category) and (
                    (QuizResultsTable.score greater score) or (
                        (QuizResultsTable.score eq score) and (QuizResultsTable.playedAt less now)
                    )
                )
            }
            .count()
            .toInt()

        val ranking = betterCount + 1
        val dto = QuizResultDto(id = insertedId, category = category, score = score, playedAt = now)
        dto to ranking
    }

    fun userResults(userId: Int, page: Int, pageSize: Int): PaginatedResponse<QuizResultDto> = transaction(database) {
        val baseFilter = QuizResultsTable.userId eq userId
        val totalItems = QuizResultsTable.select(baseFilter).count().toInt()
        val totalPages = if (totalItems == 0) 0 else (totalItems + pageSize - 1) / pageSize
        val items = QuizResultsTable.select(baseFilter)
            .orderBy(QuizResultsTable.playedAt to SortOrder.DESC)
            .limit(pageSize, offset = (page.toLong() - 1) * pageSize)
            .map {
                QuizResultDto(
                    id = it[QuizResultsTable.id],
                    category = it[QuizResultsTable.category],
                    score = it[QuizResultsTable.score],
                    playedAt = it[QuizResultsTable.playedAt],
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
}
