package com.booru.client.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Represents a single scraped post from Gelbooru.
 * All fields are nullable because scraped HTML may be incomplete.
 */
data class PostModel(
    @SerializedName("post_id")
    val postId: Int? = null,

    @SerializedName("preview_url")
    val previewUrl: String? = null,

    @SerializedName("sample_url")
    val sampleUrl: String? = null,

    @SerializedName("file_url")
    val fileUrl: String? = null,

    @SerializedName("tags")
    val tags: String? = null,

    @SerializedName("score")
    val score: Int? = null,

    @SerializedName("rating")
    val rating: String? = null,

    @SerializedName("source")
    val source: String? = null
) : Serializable {

    /** Returns a compact tag list split by whitespace. */
    fun tagList(): List<String> = tags?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()

    /** Rating categories: safe, questionable, explicit, unknown */
    fun ratingCategory(): String = when (rating) {
        "s", "safe" -> "Safe"
        "q", "questionable" -> "Questionable"
        "e", "explicit" -> "Explicit"
        else -> "Unknown"
    }

    /** JSON representation using Gson. */
    fun toJson(): String = com.google.gson.Gson().toJson(this)

    companion object {
        /** Deserialize a JSON string back into a PostModel. */
        fun fromJson(json: String): PostModel =
            com.google.gson.Gson().fromJson(json, PostModel::class.java)
    }
}
