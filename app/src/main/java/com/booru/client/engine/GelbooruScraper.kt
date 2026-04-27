package com.booru.client.engine

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.booru.client.data.model.PostModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Background WebView-based scraper for Gelbooru.
 *
 * Strategy:
 * 1. Load the search URL in a headless WebView.
 * 2. Inject JS to click "Show hidden content" if present.
 * 3. Wait for DOM update, then inject parsing JS.
 * 4. Extract post data → PostModel list → JSON.
 */
class GelbooruScraper(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://gelbooru.com"
        private const val SEARCH_PATH = "/index.php?page=post&s=list&tags="
        private const val PAGE_PARAM = "&pid="

        /** JS: Toggle hidden content (gore/hidden tags) */
        private val JS_TOGGLE_HIDDEN = """
            (function() {
                try {
                    var btn = document.querySelector('#confirm-shown, .sidebar-shown, button[onclick*="hidden"], a[href*="shown"]');
                    if (btn) { btn.click(); return '{"toggled": true}'; }
                    document.cookie = "fringeBenefits=yup; path=/; domain=.gelbooru.com";
                    return '{"toggled": false, "cookie_set": true}';
                } catch(e) {
                    return '{"toggled": false, "error": "' + e.message + '"}';
                }
            })();
        """.trimIndent()

        /** JS: Parse post list DOM and return JSON array */
        private val JS_PARSE_POSTS = """
            (function() {
                try {
                    var posts = [];
                    var containers = document.querySelectorAll('.thumbnail-preview, .post-preview, article, .image-card');
                    if (containers.length === 0) {
                        containers = document.querySelectorAll('[id^="p"], .thumb, a[href*="s=view"]');
                    }
                    containers.forEach(function(el) {
                        try {
                            var postId = null;
                            var previewUrl = '';
                            var sampleUrl = '';
                            var fileUrl = '';
                            var tags = '';
                            var score = 0;
                            var rating = '';
                            var idAttr = el.getAttribute('id') || '';
                            var idMatch = idAttr.match(/p(\d+)/);
                            if (idMatch) postId = parseInt(idMatch[1]);
                            if (!postId) {
                                var link = el.querySelector('a[href*="s=view"]');
                                if (link) {
                                    var href = link.getAttribute('href') || '';
                                    var hrefMatch = href.match(/id=(\d+)/);
                                    if (hrefMatch) postId = parseInt(hrefMatch[1]);
                                }
                            }
                            var img = el.querySelector('img');
                            if (img) {
                                previewUrl = img.getAttribute('src') || img.getAttribute('data-src') || '';
                                tags = img.getAttribute('alt') || img.getAttribute('title') || '';
                                if (!previewUrl) previewUrl = img.getAttribute('data-original') || '';
                            }
                            var sampleEl = el.querySelector('[data-sample-url], [data-large-file-url]');
                            if (sampleEl) {
                                sampleUrl = sampleEl.getAttribute('data-sample-url')
                                          || sampleEl.getAttribute('data-large-file-url') || '';
                            }
                            var text = el.textContent || '';
                            var scoreMatch = text.match(/Score:\s*(\d+)/);
                            if (scoreMatch) score = parseInt(scoreMatch[1]);
                            var ratingMatch = text.match(/Rating:\s*(safe|questionable|explicit)/i);
                            if (ratingMatch) rating = ratingMatch[1].toLowerCase();
                            if (postId !== null && postId > 0) {
                                posts.push({
                                    post_id: postId,
                                    preview_url: previewUrl,
                                    sample_url: sampleUrl,
                                    file_url: fileUrl,
                                    tags: tags.trim(),
                                    score: score,
                                    rating: rating
                                });
                            }
                        } catch(innerE) {}
                    });
                    if (posts.length === 0) {
                        var scripts = document.querySelectorAll('script');
                        scripts.forEach(function(s) {
                            var content = s.textContent || '';
                            var postRegex = /(?:post_id|id)["']?\s*:\s*(\d+)/g;
                            var match;
                            while ((match = postRegex.exec(content)) !== null) {
                                var pid = parseInt(match[1]);
                                if (!posts.find(function(p) { return p.post_id === pid; })) {
                                    var urlMatch = content.match(/(?:file_url|image|directory)["']?\s*:\s*["']([^"']+)["']/);
                                    posts.push({
                                        post_id: pid,
                                        preview_url: urlMatch ? urlMatch[1] : '',
                                        sample_url: '',
                                        file_url: urlMatch ? urlMatch[1] : '',
                                        tags: '',
                                        score: 0,
                                        rating: ''
                                    });
                                }
                            }
                        });
                    }
                    return JSON.stringify(posts);
                } catch(e) {
                    return JSON.stringify([{error: e.message}]);
                }
            })();
        """.trimIndent()
    }

    /**
     * Parses raw JSON from the JS scraper into PostModel objects.
     * Pure-logic parser, testable without Android dependencies.
     */
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

    /**
     * Evaluates JavaScript in a WebView and returns the result.
     * Uses CountDownLatch to bridge the async callback to a sync result.
     */
    private fun evaluateJsSync(webView: WebView, script: String): String {
        val latch = CountDownLatch(1)
        val resultHolder = arrayOf<String?>(null)

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            webView.evaluateJavascript(script) { result ->
                val cleaned = result
                    ?.trim('"')
                    ?.replace("\\\"", "\"")
                    ?.replace("\\\\", "\\")
                    ?: ""
                resultHolder[0] = cleaned
                latch.countDown()
            }
        }

        latch.await()
        return resultHolder[0] ?: ""
    }

    /**
     * Main scrape function. Returns a list of PostModel from Gelbooru.
     *
     * @param tags Search tags (space-separated)
     * @param page Page number (0-indexed)
     * @return List of PostModel
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun scrapePosts(tags: String = "", page: Int = 0): List<PostModel> {
        return withContext(Dispatchers.IO) {
            val url = buildSearchUrl(tags, page)

            suspendCancellableCoroutine { continuation ->
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    val webView = WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString =
                            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
                        settings.loadsImagesAutomatically = false
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                    }

                    // Set hidden content cookie
                    CookieManager.getInstance().setCookie(
                        "https://gelbooru.com",
                        "fringeBenefits=yup"
                    )

                    var destroyed = false

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, loadedUrl: String?) {
                            if (destroyed) return
                            Thread {
                                try {
                                    // Step 1: Toggle hidden content
                                    evaluateJsSync(view, JS_TOGGLE_HIDDEN)

                                    // Step 2: Wait for DOM update
                                    Thread.sleep(800)

                                    // Step 3: Parse posts
                                    val rawJson = evaluateJsSync(view, JS_PARSE_POSTS)
                                    val posts = parseJsonToPosts(rawJson)

                                    destroyed = true
                                    handler.post { view.destroy() }
                                    continuation.resume(posts)
                                } catch (e: Exception) {
                                    if (!destroyed) {
                                        destroyed = true
                                        handler.post { view.destroy() }
                                    }
                                    continuation.resumeWithException(e)
                                }
                            }.start()
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean = false
                    }

                    webView.loadUrl(url)
                }
            }
        }
    }

    /**
     * Builds the full Gelbooru search URL.
     */
    private fun buildSearchUrl(tags: String, page: Int): String {
        val tagStr = tags.trim().replace(" ", "+")
        val pid = page * 42
        return "$BASE_URL$SEARCH_PATH$tagStr$PAGE_PARAM$pid"
    }
}
