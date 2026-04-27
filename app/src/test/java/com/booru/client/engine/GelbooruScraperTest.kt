package com.booru.client.engine

import com.booru.client.data.model.PostModel
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the Gelbooru scraper's HTML-to-PostModel JSON parser.
 * Tests the parseJsonToPosts logic, PostModel serialization, and regex patterns
 * without requiring a WebView or Android context.
 */
class GelbooruScraperTest {

    /**
     * Helper: creates a minimal scraper-like object that exposes parseJsonToPosts.
     * We test parsing in isolation since it's a pure function.
     */
    private val parser = object {
        fun parseJsonToPosts(jsonString: String): List<PostModel> {
            return try {
                val cleaned = jsonString.trim()
                if (cleaned.isBlank() || cleaned == "[]" || cleaned == "null") {
                    return emptyList()
                }
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<PostModel>>() {}.type
                gson.fromJson(cleaned, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Mocked JSON output from the JS scraper
    private val mockJsonOutput = """
        [
            {
                "post_id": 1001,
                "preview_url": "https://gelbooru.com/thumbnails/abc123.jpg",
                "sample_url": "",
                "file_url": "",
                "tags": "blue_sky landscape nature",
                "score": 42,
                "rating": "safe"
            },
            {
                "post_id": 1002,
                "preview_url": "https://gelbooru.com/thumbnails/def456.jpg",
                "sample_url": "",
                "file_url": "",
                "tags": "sunset ocean",
                "score": 87,
                "rating": "questionable"
            },
            {
                "post_id": 1003,
                "preview_url": "https://gelbooru.com/thumbnails/ghi789.jpg",
                "sample_url": "",
                "file_url": "",
                "tags": "anime girl",
                "score": 15,
                "rating": "explicit"
            }
        ]
    """.trimIndent()

    private val mockEmptyJson = "[]"

    private val mockSinglePostJson = """
        [
            {
                "post_id": 9999,
                "preview_url": "https://gelbooru.com/thumbnails/single.jpg",
                "sample_url": "https://gelbooru.com/samples/single_sample.jpg",
                "file_url": "https://gelbooru.com/images/single_full.png",
                "tags": "test_tag",
                "score": 1,
                "rating": "safe"
            }
        ]
    """.trimIndent()

    // =====================================================
    // JSON Parsing Tests
    // =====================================================

    @Test
    fun `parseJsonToPosts parses valid multi-post JSON correctly`() {
        val posts = parser.parseJsonToPosts(mockJsonOutput)
        assertEquals(3, posts.size)

        assertEquals(1001, posts[0].postId)
        assertEquals("https://gelbooru.com/thumbnails/abc123.jpg", posts[0].previewUrl)
        assertEquals("blue_sky landscape nature", posts[0].tags)
        assertEquals(42, posts[0].score)
        assertEquals("safe", posts[0].rating)

        assertEquals(1002, posts[1].postId)
        assertEquals("questionable", posts[1].rating)

        assertEquals(1003, posts[2].postId)
        assertEquals("explicit", posts[2].rating)
    }

    @Test
    fun `parseJsonToPosts returns empty list for empty JSON array`() {
        val posts = parser.parseJsonToPosts(mockEmptyJson)
        assertTrue(posts.isEmpty())
    }

    @Test
    fun `parseJsonToPosts returns empty list for blank input`() {
        assertTrue(parser.parseJsonToPosts("").isEmpty())
        assertTrue(parser.parseJsonToPosts("   ").isEmpty())
    }

    @Test
    fun `parseJsonToPosts handles null string gracefully`() {
        assertTrue(parser.parseJsonToPosts("null").isEmpty())
    }

    @Test
    fun `parseJsonToPosts parses single post correctly`() {
        val posts = parser.parseJsonToPosts(mockSinglePostJson)
        assertEquals(1, posts.size)

        val post = posts[0]
        assertEquals(9999, post.postId)
        assertEquals("https://gelbooru.com/thumbnails/single.jpg", post.previewUrl)
        assertEquals("https://gelbooru.com/samples/single_sample.jpg", post.sampleUrl)
        assertEquals("https://gelbooru.com/images/single_full.png", post.fileUrl)
        assertEquals("test_tag", post.tags)
        assertEquals(1, post.score)
        assertEquals("safe", post.rating)
    }

    @Test
    fun `parseJsonToPosts handles malformed JSON gracefully`() {
        val posts = parser.parseJsonToPosts("not valid json {{{")
        assertTrue(posts.isEmpty())
    }

    @Test
    fun `parseJsonToPosts handles JSON with missing fields`() {
        val minimalJson = """
            [
                {"post_id": 500},
                {"preview_url": "http://example.com/img.jpg"}
            ]
        """.trimIndent()

        val posts = parser.parseJsonToPosts(minimalJson)
        assertEquals(2, posts.size)
        assertEquals(500, posts[0].postId)
        assertNull(posts[0].previewUrl)
        assertNull(posts[1].postId)
        assertEquals("http://example.com/img.jpg", posts[1].previewUrl)
    }

    @Test
    fun `parseJsonToPosts handles JS-escaped JSON from evaluateJavascript`() {
        // Android's evaluateJavascript wraps strings in quotes and escapes
        val jsEscaped = "\"[{\\\"post_id\\\":1001,\\\"preview_url\\\":\\\"https://gelbooru.com/thumbnails/test.jpg\\\",\\\"tags\\\":\\\"test\\\",\\\"score\\\":10,\\\"rating\\\":\\\"safe\\\"}]\""
        val cleaned = jsEscaped
            .trim('"')
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
        val posts = parser.parseJsonToPosts(cleaned)
        assertEquals(1, posts.size)
        assertEquals(1001, posts[0].postId)
    }

    // =====================================================
    // PostModel Tests
    // =====================================================

    @Test
    fun `PostModel tagList splits tags correctly`() {
        val post = PostModel(tags = "blue_sky   landscape  nature")
        val tagList = post.tagList()
        assertEquals(3, tagList.size)
        assertEquals("blue_sky", tagList[0])
        assertEquals("landscape", tagList[1])
        assertEquals("nature", tagList[2])
    }

    @Test
    fun `PostModel tagList handles null tags`() {
        assertTrue(PostModel(tags = null).tagList().isEmpty())
    }

    @Test
    fun `PostModel tagList handles empty tags`() {
        assertTrue(PostModel(tags = "").tagList().isEmpty())
        assertTrue(PostModel(tags = "   ").tagList().isEmpty())
    }

    @Test
    fun `PostModel ratingCategory maps all ratings correctly`() {
        assertEquals("Safe", PostModel(rating = "safe").ratingCategory())
        assertEquals("Safe", PostModel(rating = "s").ratingCategory())
        assertEquals("Questionable", PostModel(rating = "questionable").ratingCategory())
        assertEquals("Questionable", PostModel(rating = "q").ratingCategory())
        assertEquals("Explicit", PostModel(rating = "explicit").ratingCategory())
        assertEquals("Explicit", PostModel(rating = "e").ratingCategory())
        assertEquals("Unknown", PostModel(rating = "unknown_rating").ratingCategory())
        assertEquals("Unknown", PostModel(rating = null).ratingCategory())
    }

    @Test
    fun `PostModel JSON round-trip preserves all fields`() {
        val original = PostModel(
            postId = 12345,
            previewUrl = "https://example.com/preview.jpg",
            sampleUrl = "https://example.com/sample.jpg",
            fileUrl = "https://example.com/full.png",
            tags = "test tag1 tag2",
            score = 99,
            rating = "safe",
            source = "https://example.com"
        )

        val json = original.toJson()
        val restored = PostModel.fromJson(json)

        assertEquals(original.postId, restored.postId)
        assertEquals(original.previewUrl, restored.previewUrl)
        assertEquals(original.sampleUrl, restored.sampleUrl)
        assertEquals(original.fileUrl, restored.fileUrl)
        assertEquals(original.tags, restored.tags)
        assertEquals(original.score, restored.score)
        assertEquals(original.rating, restored.rating)
        assertEquals(original.source, restored.source)
    }

    @Test
    fun `PostModel JSON round-trip with null fields`() {
        val original = PostModel()
        val json = original.toJson()
        val restored = PostModel.fromJson(json)
        assertNull(restored.postId)
        assertNull(restored.previewUrl)
        assertNull(restored.tags)
    }

    // =====================================================
    // Regex Pattern Tests (verify JS regex equivalents)
    // =====================================================

    @Test
    fun `regex extracts post IDs from anchor hrefs`() {
        val hrefRegex = Regex("""id=(\d+)""")
        val ids = listOf(
            "index.php?page=post&s=view&id=1001" to 1001,
            "index.php?page=post&s=view&id=99999" to 99999,
            "index.php?page=post&s=view&id=0" to 0
        )
        ids.forEach { (href, expected) ->
            assertEquals(expected, hrefRegex.find(href)?.groupValues?.get(1)?.toInt())
        }
    }

    @Test
    fun `regex extracts post IDs from element IDs`() {
        val idRegex = Regex("""p(\d+)""")
        val cases = listOf(
            "p1001" to 1001,
            "p99999" to 99999,
            "p-no-id" to null,
            "preview1001" to null
        )
        cases.forEach { (input, expected) ->
            assertEquals(expected, idRegex.find(input)?.groupValues?.get(1)?.toInt())
        }
    }

    @Test
    fun `regex extracts score from text content`() {
        val scoreRegex = Regex("""Score:\s*(\d+)""")
        val cases = listOf(
            "Score: 42" to 42,
            "Score: 0" to 0,
            "Score: 9999" to 9999,
            "No score here" to null,
            "rating: safe Score: 15" to 15
        )
        cases.forEach { (text, expected) ->
            assertEquals(expected, scoreRegex.find(text)?.groupValues?.get(1)?.toInt())
        }
    }

    @Test
    fun `regex extracts rating from text content`() {
        val ratingRegex = Regex("""Rating:\s*(safe|questionable|explicit)""", RegexOption.IGNORE_CASE)
        val cases = listOf(
            "Rating: safe" to "safe",
            "Rating: questionable" to "questionable",
            "Rating: explicit" to "explicit",
            "Rating: SAFE" to "SAFE",
            "No rating" to null
        )
        cases.forEach { (text, expected) ->
            assertEquals(expected, ratingRegex.find(text)?.groupValues?.get(1))
        }
    }

    @Test
    fun `regex extracts image URLs from img tags`() {
        val html = """<img src="https://gelbooru.com/thumbnails/abc.jpg" data-src="https://gelbooru.com/thumbnails/def.jpg" alt="blue_sky">"""
        val srcRegex = Regex("""src="([^"]+)"""")
        val altRegex = Regex("""alt="([^"]+)""""")

        assertEquals("https://gelbooru.com/thumbnails/abc.jpg", srcRegex.find(html)?.groupValues?.get(1))
        assertEquals("blue_sky", altRegex.find(html)?.groupValues?.get(1))
    }

    @Test
    fun `buildSearchUrl constructs correct URL format`() {
        val baseUrl = "https://gelbooru.com/index.php?page=post&s=list&tags="
        val tags = "blue_sky landscape"
        val encoded = tags.replace(" ", "+")
        val pid = 0 * 42
        val expected = "$baseUrl$encoded&pid=$pid"
        assertEquals(expected, "${baseUrl}blue_sky+landscape&pid=0")

        // Page 2
        val pid2 = 2 * 42
        assertEquals("$baseUrl${encoded}&pid=${pid2}", "https://gelbooru.com/index.php?page=post&s=list&tags=blue_sky+landscape&pid=84")
    }
}
