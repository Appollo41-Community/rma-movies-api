package rs.raf.edu.rma.beskar.domain

import kotlinx.serialization.Serializable

@Serializable
data class PostListItem(
    val id: Int,
    val title: String,
    val description: String,
    val mediaType: String,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val mediaAuthor: String,
    val date: String,
    val likes: Int,
    val dislikes: Int,
    val commentsCount: Int,
)

@Serializable
data class Post(
    val id: Int,
    val title: String,
    val description: String,
    val mediaType: String,
    val imageUrl: String? = null,
    val imageHdUrl: String? = null,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val mediaAuthor: String,
    val descriptionAuthor: String? = null,
    val copyright: String? = null,
    val altText: String? = null,
    val date: String,
    val likes: Int,
    val dislikes: Int,
    val commentsCount: Int,
    val categories: List<Category>,
    val references: List<PostReference>,
)

@Serializable
data class Category(
    val id: Int,
    val name: String,
)

@Serializable
data class PostReference(
    val id: Int,
    val title: String,
    val date: String,
)

@Serializable
data class CategoryWithCount(
    val id: Int,
    val name: String,
    val postCount: Int,
)

@Serializable
data class Comment(
    val id: Int,
    val postId: Int,
    val nickname: String,
    val text: String,
    val createdAt: String,
)

@Serializable
data class PostCollection(
    val id: Int,
    val name: String,
    val nickname: String,
    val createdAt: String,
    val posts: List<PostListItem>,
)

@Serializable
data class BeskarStats(
    val totalPosts: Int,
    val totalImages: Int,
    val totalVideos: Int,
    val totalLikes: Int,
    val totalComments: Int,
    val dateRange: DateRange,
    val totalCategories: Int,
)

@Serializable
data class DateRange(
    val from: String,
    val to: String,
)

@Serializable
data class ReactionRequest(
    val nickname: String,
)

@Serializable
data class CommentRequest(
    val nickname: String,
    val text: String,
)

@Serializable
data class CreateCollectionRequest(
    val nickname: String,
    val name: String,
    val postIds: List<Int> = emptyList(),
)

@Serializable
data class UpdateCollectionRequest(
    val name: String,
    val postIds: List<Int> = emptyList(),
)
