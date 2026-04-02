package rs.raf.edu.rma.beskar.repository

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import rs.raf.edu.rma.beskar.domain.*
import rs.raf.edu.rma.movies.domain.PaginatedResponse
import java.time.Instant

class BeskarRepository(private val database: Database) {

    // -----------------------------------------------------------------------
    // Posts — Read
    // -----------------------------------------------------------------------

    fun getPosts(
        page: Int = 1,
        pageSize: Int = 20,
        query: String? = null,
        mediaType: String? = null,
        categoryId: Int? = null,
        startDate: String? = null,
        endDate: String? = null,
        sortBy: String = "date",
        sortOrder: String = "desc",
    ): PaginatedResponse<PostListItem> = transaction(database) {
        val baseQuery = if (categoryId != null) {
            PostsTable.innerJoin(PostCategoriesTable, { PostsTable.id }, { PostCategoriesTable.postId })
                .slice(PostsTable.columns)
                .selectAll()
                .andWhere { PostCategoriesTable.categoryId eq categoryId }
        } else {
            PostsTable.selectAll()
        }

        if (!query.isNullOrBlank()) {
            val sanitized = query.replace("%", "\\%").replace("_", "\\_")
            baseQuery.andWhere {
                (PostsTable.title like "%${sanitized}%") or
                        (PostsTable.description like "%${sanitized}%")
            }
        }

        if (mediaType != null) {
            baseQuery.andWhere { PostsTable.mediaType eq mediaType }
        }
        if (startDate != null) {
            baseQuery.andWhere { PostsTable.date greaterEq startDate }
        }
        if (endDate != null) {
            baseQuery.andWhere { PostsTable.date lessEq endDate }
        }

        val totalItems = baseQuery.count().toInt()
        val totalPages = if (totalItems == 0) 0 else (totalItems + pageSize - 1) / pageSize

        val order = if (sortOrder.equals("asc", ignoreCase = true)) SortOrder.ASC else SortOrder.DESC

        val isComputedSort = sortBy in listOf("likes", "dislikes", "comments_count")

        if (!isComputedSort) {
            val sortColumn: Column<*> = when (sortBy) {
                "title" -> PostsTable.title
                else -> PostsTable.date
            }

            val rows = baseQuery.copy()
                .orderBy(sortColumn to order)
                .limit(pageSize, offset = (page.toLong() - 1) * pageSize)
                .toList()

            val postIds = rows.map { it[PostsTable.id] }
            val reactionCounts = getReactionCounts(postIds)
            val commentCounts = getCommentCounts(postIds)

            val items = rows.map { row -> row.toPostListItem(reactionCounts, commentCounts) }

            PaginatedResponse(
                page = page,
                pageSize = pageSize,
                totalItems = totalItems,
                totalPages = totalPages,
                items = items,
            )
        } else {
            val allRows = baseQuery.copy().toList()
            val allPostIds = allRows.map { it[PostsTable.id] }
            val reactionCounts = getReactionCounts(allPostIds)
            val commentCounts = getCommentCounts(allPostIds)

            val allItems = allRows.map { row -> row.toPostListItem(reactionCounts, commentCounts) }

            val comparator: Comparator<PostListItem> = when (sortBy) {
                "likes" -> compareBy { it.likes }
                "dislikes" -> compareBy { it.dislikes }
                else -> compareBy { it.commentsCount }
            }

            val sorted = if (order == SortOrder.DESC) allItems.sortedWith(comparator.reversed())
            else allItems.sortedWith(comparator)

            val offset = (page - 1) * pageSize
            val items = sorted.drop(offset).take(pageSize)

            PaginatedResponse(
                page = page,
                pageSize = pageSize,
                totalItems = totalItems,
                totalPages = totalPages,
                items = items,
            )
        }
    }

    fun getPostById(id: Int): Post? = transaction(database) {
        buildPost(id)
    }

    fun getPostByDate(date: String): Post? = transaction(database) {
        val row = PostsTable.select(PostsTable.date eq date)
            .singleOrNull() ?: return@transaction null
        buildPost(row[PostsTable.id])
    }

    fun getRandomPost(): Post? = transaction(database) {
        val row = PostsTable.selectAll()
            .orderBy(Random())
            .limit(1)
            .singleOrNull() ?: return@transaction null
        buildPost(row[PostsTable.id])
    }

    fun getOnThisDay(month: Int, day: Int): List<PostListItem> = transaction(database) {
        val monthStr = month.toString().padStart(2, '0')
        val dayStr = day.toString().padStart(2, '0')
        val pattern = "%-${monthStr}-${dayStr}"

        val rows = PostsTable.select { PostsTable.date like pattern }
            .orderBy(PostsTable.date to SortOrder.DESC)
            .toList()

        val postIds = rows.map { it[PostsTable.id] }
        val reactionCounts = getReactionCounts(postIds)
        val commentCounts = getCommentCounts(postIds)

        rows.map { row -> row.toPostListItem(reactionCounts, commentCounts) }
    }

    fun getPopularPosts(
        page: Int = 1,
        pageSize: Int = 20,
        sortBy: String = "likes",
    ): PaginatedResponse<PostListItem> = transaction(database) {
        val allRows = PostsTable.selectAll().toList()
        val allPostIds = allRows.map { it[PostsTable.id] }
        val reactionCounts = getReactionCounts(allPostIds)
        val commentCounts = getCommentCounts(allPostIds)

        val allItems = allRows.map { row -> row.toPostListItem(reactionCounts, commentCounts) }

        val comparator: Comparator<PostListItem> = when (sortBy) {
            "dislikes" -> compareBy { it.dislikes }
            "comments_count" -> compareBy { it.commentsCount }
            else -> compareBy { it.likes }
        }

        val sorted = allItems.sortedWith(comparator.reversed())
        val totalItems = sorted.size
        val totalPages = if (totalItems == 0) 0 else (totalItems + pageSize - 1) / pageSize
        val offset = (page - 1) * pageSize
        val items = sorted.drop(offset).take(pageSize)

        PaginatedResponse(
            page = page,
            pageSize = pageSize,
            totalItems = totalItems,
            totalPages = totalPages,
            items = items,
        )
    }

    fun getRelatedPosts(postId: Int): List<PostListItem> = transaction(database) {
        val categoryIds = PostCategoriesTable
            .select { PostCategoriesTable.postId eq postId }
            .map { it[PostCategoriesTable.categoryId] }

        if (categoryIds.isEmpty()) return@transaction emptyList()

        val rows = PostCategoriesTable
            .innerJoin(PostsTable, { PostCategoriesTable.postId }, { PostsTable.id })
            .slice(PostsTable.columns)
            .select { (PostCategoriesTable.categoryId inList categoryIds) and (PostsTable.id neq postId) }
            .withDistinct()
            .orderBy(PostsTable.date to SortOrder.DESC)
            .limit(20)
            .toList()

        val relatedIds = rows.map { it[PostsTable.id] }
        val reactionCounts = getReactionCounts(relatedIds)
        val commentCounts = getCommentCounts(relatedIds)

        rows.map { row -> row.toPostListItem(reactionCounts, commentCounts) }
    }

    // -----------------------------------------------------------------------
    // Reactions
    // -----------------------------------------------------------------------

    fun likePost(postId: Int, nickname: String): Unit = transaction(database) {
        val existing = PostReactionsTable
            .select { (PostReactionsTable.postId eq postId) and (PostReactionsTable.nickname eq nickname) }
            .singleOrNull()

        val now = Instant.now().toString()

        if (existing != null) {
            val existingType = existing[PostReactionsTable.type]
            if (existingType == "like") {
                throw AlreadyReactedException("Already liked")
            }
            PostReactionsTable.update({ PostReactionsTable.id eq existing[PostReactionsTable.id] }) {
                it[type] = "like"
                it[createdAt] = now
            }
        } else {
            try {
                PostReactionsTable.insert {
                    it[PostReactionsTable.postId] = postId
                    it[PostReactionsTable.nickname] = nickname
                    it[type] = "like"
                    it[createdAt] = now
                }
            } catch (e: Exception) {
                if (e.message?.contains("UNIQUE constraint") == true) {
                    throw AlreadyReactedException("Already reacted")
                }
                throw e
            }
        }
    }

    fun dislikePost(postId: Int, nickname: String): Unit = transaction(database) {
        val existing = PostReactionsTable
            .select { (PostReactionsTable.postId eq postId) and (PostReactionsTable.nickname eq nickname) }
            .singleOrNull()

        val now = Instant.now().toString()

        if (existing != null) {
            val existingType = existing[PostReactionsTable.type]
            if (existingType == "dislike") {
                throw AlreadyReactedException("Already disliked")
            }
            PostReactionsTable.update({ PostReactionsTable.id eq existing[PostReactionsTable.id] }) {
                it[type] = "dislike"
                it[createdAt] = now
            }
        } else {
            try {
                PostReactionsTable.insert {
                    it[PostReactionsTable.postId] = postId
                    it[PostReactionsTable.nickname] = nickname
                    it[type] = "dislike"
                    it[createdAt] = now
                }
            } catch (e: Exception) {
                if (e.message?.contains("UNIQUE constraint") == true) {
                    throw AlreadyReactedException("Already reacted")
                }
                throw e
            }
        }
    }

    fun removeLike(postId: Int, nickname: String): Unit = transaction(database) {
        val deleted = PostReactionsTable.deleteWhere {
            (PostReactionsTable.postId eq postId) and
                    (PostReactionsTable.nickname eq nickname) and
                    (PostReactionsTable.type eq "like")
        }
        if (deleted == 0) throw ReactionNotFoundException("Like not found")
    }

    fun removeDislike(postId: Int, nickname: String): Unit = transaction(database) {
        val deleted = PostReactionsTable.deleteWhere {
            (PostReactionsTable.postId eq postId) and
                    (PostReactionsTable.nickname eq nickname) and
                    (PostReactionsTable.type eq "dislike")
        }
        if (deleted == 0) throw ReactionNotFoundException("Dislike not found")
    }

    // -----------------------------------------------------------------------
    // Comments
    // -----------------------------------------------------------------------

    fun getComments(
        postId: Int,
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<Comment> = transaction(database) {
        val baseQuery = CommentsTable.select { CommentsTable.postId eq postId }

        val totalItems = baseQuery.count().toInt()
        val totalPages = if (totalItems == 0) 0 else (totalItems + pageSize - 1) / pageSize

        val items = baseQuery.copy()
            .orderBy(CommentsTable.createdAt to SortOrder.DESC)
            .limit(pageSize, offset = (page.toLong() - 1) * pageSize)
            .map { row ->
                Comment(
                    id = row[CommentsTable.id],
                    postId = row[CommentsTable.postId],
                    nickname = row[CommentsTable.nickname],
                    text = row[CommentsTable.text],
                    createdAt = row[CommentsTable.createdAt],
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

    fun addComment(postId: Int, nickname: String, text: String): Comment = transaction(database) {
        val now = Instant.now().toString()
        val id = CommentsTable.insert {
            it[CommentsTable.postId] = postId
            it[CommentsTable.nickname] = nickname
            it[CommentsTable.text] = text
            it[createdAt] = now
        } get CommentsTable.id

        Comment(
            id = id,
            postId = postId,
            nickname = nickname,
            text = text,
            createdAt = now,
        )
    }

    // -----------------------------------------------------------------------
    // Collections
    // -----------------------------------------------------------------------

    fun validatePostIds(postIds: List<Int>): List<Int> = transaction(database) {
        if (postIds.isEmpty()) return@transaction emptyList()
        val existing = PostsTable
            .slice(PostsTable.id)
            .select { PostsTable.id inList postIds }
            .map { it[PostsTable.id] }
            .toSet()
        postIds.filter { it !in existing }
    }

    fun createCollection(nickname: String, name: String, postIds: List<Int>): PostCollection = transaction(database) {
        val now = Instant.now().toString()
        val id = CollectionsTable.insert {
            it[CollectionsTable.name] = name
            it[CollectionsTable.nickname] = nickname
            it[createdAt] = now
        } get CollectionsTable.id

        for (postId in postIds) {
            CollectionPostsTable.insert {
                it[collectionId] = id
                it[CollectionPostsTable.postId] = postId
            }
        }

        getCollectionByIdInternal(id)!!
    }

    fun getCollectionById(id: Int): PostCollection? = transaction(database) {
        getCollectionByIdInternal(id)
    }

    fun updateCollection(id: Int, name: String, postIds: List<Int>): PostCollection = transaction(database) {
        CollectionsTable.update({ CollectionsTable.id eq id }) {
            it[CollectionsTable.name] = name
        }

        CollectionPostsTable.deleteWhere { CollectionPostsTable.collectionId eq id }
        for (postId in postIds) {
            CollectionPostsTable.insert {
                it[collectionId] = id
                it[CollectionPostsTable.postId] = postId
            }
        }

        getCollectionByIdInternal(id)!!
    }

    // -----------------------------------------------------------------------
    // Categories
    // -----------------------------------------------------------------------

    fun getCategories(
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<CategoryWithCount> = transaction(database) {
        val countExpr = PostCategoriesTable.postId.count()

        val totalItems = CategoriesTable.selectAll().count().toInt()
        val totalPages = if (totalItems == 0) 0 else (totalItems + pageSize - 1) / pageSize

        val items = CategoriesTable
            .leftJoin(PostCategoriesTable, { CategoriesTable.id }, { PostCategoriesTable.categoryId })
            .slice(CategoriesTable.id, CategoriesTable.name, countExpr)
            .selectAll()
            .groupBy(CategoriesTable.id, CategoriesTable.name)
            .orderBy(CategoriesTable.name to SortOrder.ASC)
            .limit(pageSize, offset = (page.toLong() - 1) * pageSize)
            .map {
                CategoryWithCount(
                    id = it[CategoriesTable.id],
                    name = it[CategoriesTable.name],
                    postCount = it[countExpr].toInt(),
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

    fun getPopularCategories(limit: Int = 20): List<CategoryWithCount> = transaction(database) {
        val countExpr = PostCategoriesTable.postId.count()
        CategoriesTable
            .leftJoin(PostCategoriesTable, { CategoriesTable.id }, { PostCategoriesTable.categoryId })
            .slice(CategoriesTable.id, CategoriesTable.name, countExpr)
            .selectAll()
            .groupBy(CategoriesTable.id, CategoriesTable.name)
            .orderBy(countExpr to SortOrder.DESC)
            .limit(limit)
            .map {
                CategoryWithCount(
                    id = it[CategoriesTable.id],
                    name = it[CategoriesTable.name],
                    postCount = it[countExpr].toInt(),
                )
            }
    }

    // -----------------------------------------------------------------------
    // Stats
    // -----------------------------------------------------------------------

    fun getStats(): BeskarStats = transaction(database) {
        val totalPosts = PostsTable.selectAll().count().toInt()
        val totalImages = PostsTable.select { PostsTable.mediaType eq "image" }.count().toInt()
        val totalVideos = PostsTable.select { PostsTable.mediaType eq "video" }.count().toInt()
        val totalLikes = PostReactionsTable.select { PostReactionsTable.type eq "like" }.count().toInt()
        val totalComments = CommentsTable.selectAll().count().toInt()
        val minDate = PostsTable.selectAll()
            .orderBy(PostsTable.date to SortOrder.ASC)
            .limit(1)
            .singleOrNull()?.get(PostsTable.date) ?: ""
        val maxDate = PostsTable.selectAll()
            .orderBy(PostsTable.date to SortOrder.DESC)
            .limit(1)
            .singleOrNull()?.get(PostsTable.date) ?: ""
        val totalCategories = CategoriesTable.selectAll().count().toInt()

        BeskarStats(
            totalPosts = totalPosts,
            totalImages = totalImages,
            totalVideos = totalVideos,
            totalLikes = totalLikes,
            totalComments = totalComments,
            dateRange = DateRange(from = minDate, to = maxDate),
            totalCategories = totalCategories,
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    fun postExists(id: Int): Boolean = transaction(database) {
        PostsTable.select(PostsTable.id eq id).count() > 0
    }

    fun collectionExists(id: Int): Boolean = transaction(database) {
        CollectionsTable.select(CollectionsTable.id eq id).count() > 0
    }

    private fun buildPost(id: Int): Post? {
        val row = PostsTable.select(PostsTable.id eq id)
            .singleOrNull() ?: return null

        val categories = getCategoriesForPost(id)
        val reactions = getReactionCounts(listOf(id))[id]
        val commentCount = getCommentCounts(listOf(id))[id] ?: 0
        val references = getReferencesForPost(id)

        return Post(
            id = row[PostsTable.id],
            title = row[PostsTable.title],
            description = row[PostsTable.description],
            mediaType = row[PostsTable.mediaType],
            imageUrl = row[PostsTable.imageUrl],
            imageHdUrl = row[PostsTable.imageHdUrl],
            videoUrl = row[PostsTable.videoUrl],
            thumbnailUrl = row[PostsTable.thumbnailUrl],
            mediaAuthor = row[PostsTable.mediaAuthor],
            descriptionAuthor = row[PostsTable.descriptionAuthor],
            copyright = row[PostsTable.copyright],
            altText = row[PostsTable.altText],
            date = row[PostsTable.date],
            likes = reactions?.first ?: 0,
            dislikes = reactions?.second ?: 0,
            commentsCount = commentCount,
            categories = categories,
            references = references,
        )
    }

    private fun getCollectionByIdInternal(id: Int): PostCollection? {
        val row = CollectionsTable.select(CollectionsTable.id eq id)
            .singleOrNull() ?: return null

        val postIds = CollectionPostsTable
            .select { CollectionPostsTable.collectionId eq id }
            .map { it[CollectionPostsTable.postId] }

        val posts = if (postIds.isNotEmpty()) {
            val postRows = PostsTable.select { PostsTable.id inList postIds }
                .toList()

            val reactionCounts = getReactionCounts(postIds)
            val commentCounts = getCommentCounts(postIds)

            postRows.map { pRow -> pRow.toPostListItem(reactionCounts, commentCounts) }
        } else {
            emptyList()
        }

        return PostCollection(
            id = row[CollectionsTable.id],
            name = row[CollectionsTable.name],
            nickname = row[CollectionsTable.nickname],
            createdAt = row[CollectionsTable.createdAt],
            posts = posts,
        )
    }

    private fun getReferencesForPost(postId: Int): List<PostReference> {
        return PostReferencesTable
            .innerJoin(PostsTable, { PostReferencesTable.referencedPostId }, { PostsTable.id })
            .select { PostReferencesTable.postId eq postId }
            .map {
                PostReference(
                    id = it[PostsTable.id],
                    title = it[PostsTable.title],
                    date = it[PostsTable.date],
                )
            }
    }

    private fun getCategoriesForPost(postId: Int): List<Category> {
        return PostCategoriesTable
            .innerJoin(CategoriesTable, { PostCategoriesTable.categoryId }, { CategoriesTable.id })
            .select { PostCategoriesTable.postId eq postId }
            .map { Category(id = it[CategoriesTable.id], name = it[CategoriesTable.name]) }
    }

    private fun getReactionCounts(postIds: List<Int>): Map<Int, Pair<Int, Int>> {
        if (postIds.isEmpty()) return emptyMap()
        val countExpr = PostReactionsTable.id.count()
        val rows = PostReactionsTable
            .slice(PostReactionsTable.postId, PostReactionsTable.type, countExpr)
            .select { PostReactionsTable.postId inList postIds }
            .groupBy(PostReactionsTable.postId, PostReactionsTable.type)
            .toList()

        val result = mutableMapOf<Int, Pair<Int, Int>>()
        for (row in rows) {
            val pid = row[PostReactionsTable.postId]
            val type = row[PostReactionsTable.type]
            val count = row[countExpr].toInt()
            val current = result.getOrDefault(pid, Pair(0, 0))
            result[pid] = if (type == "like") Pair(count, current.second) else Pair(current.first, count)
        }
        return result
    }

    private fun getCommentCounts(postIds: List<Int>): Map<Int, Int> {
        if (postIds.isEmpty()) return emptyMap()
        val countExpr = CommentsTable.id.count()
        return CommentsTable
            .slice(CommentsTable.postId, countExpr)
            .select { CommentsTable.postId inList postIds }
            .groupBy(CommentsTable.postId)
            .associate { it[CommentsTable.postId] to it[countExpr].toInt() }
    }

    private fun ResultRow.toPostListItem(
        reactionCounts: Map<Int, Pair<Int, Int>>,
        commentCounts: Map<Int, Int>,
    ): PostListItem {
        val postId = this[PostsTable.id]
        val reactions = reactionCounts[postId]
        return PostListItem(
            id = postId,
            title = this[PostsTable.title],
            description = truncate(this[PostsTable.description], 150),
            mediaType = this[PostsTable.mediaType],
            imageUrl = this[PostsTable.imageUrl],
            videoUrl = this[PostsTable.videoUrl],
            thumbnailUrl = this[PostsTable.thumbnailUrl],
            mediaAuthor = this[PostsTable.mediaAuthor],
            date = this[PostsTable.date],
            likes = reactions?.first ?: 0,
            dislikes = reactions?.second ?: 0,
            commentsCount = commentCounts[postId] ?: 0,
        )
    }

    private fun truncate(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) text
        else text.take(maxLength).trimEnd() + "..."
    }
}

class AlreadyReactedException(message: String) : Exception(message)
class ReactionNotFoundException(message: String) : Exception(message)
