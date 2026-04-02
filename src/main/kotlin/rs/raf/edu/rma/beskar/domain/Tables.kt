package rs.raf.edu.rma.beskar.domain

import org.jetbrains.exposed.sql.Table

object PostsTable : Table("posts") {
    val id = integer("id").autoIncrement()
    val title = text("title")
    val description = text("description")
    val mediaType = text("media_type")
    val imageUrl = text("image_url").nullable()
    val imageHdUrl = text("image_hd_url").nullable()
    val videoUrl = text("video_url").nullable()
    val thumbnailUrl = text("thumbnail_url").nullable()
    val mediaAuthor = text("media_author")
    val descriptionAuthor = text("description_author").nullable()
    val copyright = text("copyright").nullable()
    val altText = text("alt_text").nullable()
    val date = text("date").uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

object PostReferencesTable : Table("post_references") {
    val postId = integer("post_id").references(PostsTable.id)
    val referencedPostId = integer("referenced_post_id").references(PostsTable.id)

    override val primaryKey = PrimaryKey(postId, referencedPostId)
}

object CategoriesTable : Table("categories") {
    val id = integer("id").autoIncrement()
    val name = text("name").uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

object PostCategoriesTable : Table("post_categories") {
    val postId = integer("post_id").references(PostsTable.id)
    val categoryId = integer("category_id").references(CategoriesTable.id)

    override val primaryKey = PrimaryKey(postId, categoryId)
}

object PostReactionsTable : Table("post_reactions") {
    val id = integer("id").autoIncrement()
    val postId = integer("post_id").references(PostsTable.id)
    val nickname = text("nickname")
    val type = text("type")
    val createdAt = text("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(postId, nickname)
    }
}

object CommentsTable : Table("comments") {
    val id = integer("id").autoIncrement()
    val postId = integer("post_id").references(PostsTable.id)
    val nickname = text("nickname")
    val text = text("text")
    val createdAt = text("created_at")

    override val primaryKey = PrimaryKey(id)
}

object CollectionsTable : Table("collections") {
    val id = integer("id").autoIncrement()
    val name = text("name")
    val nickname = text("nickname")
    val createdAt = text("created_at")

    override val primaryKey = PrimaryKey(id)
}

object CollectionPostsTable : Table("collection_posts") {
    val collectionId = integer("collection_id").references(CollectionsTable.id)
    val postId = integer("post_id").references(PostsTable.id)

    override val primaryKey = PrimaryKey(collectionId, postId)
}
