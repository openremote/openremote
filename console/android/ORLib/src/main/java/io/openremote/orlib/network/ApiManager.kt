package io.openremote.orlib.network

import android.net.Uri
import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.openremote.orlib.models.ORAppConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread


typealias ResponseBlock<T> = (statusCode: Int, model: T?, error: Throwable?) -> Unit

class ApiManager(private val baseUrl: String) {

    enum class HttpMethod {
        GET,
        POST,
        PUT,
    }

    fun getAppConfig(realm: String, callback: ResponseBlock<ORAppConfig>?) {
        val uri = Uri.parse(baseUrl)
        val url = URL("${uri.scheme}://${uri.host}/consoleappconfig/${realm}.json")
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = HttpMethod.GET.toString()
            setRequestProperty("Accept", "application/json")
            thread(start = true) {
                parseResponse(this, callback)
            }
        }
    }

    /*********************************Private functions*******************************/
    private fun createUrlRequest(
        method: HttpMethod,
        pathComponents: Array<String>,
        queryParameters: Map<String, Any>? = null
    ): HttpURLConnection {
        val builder = Uri.parse(baseUrl).buildUpon()

        for (pathComponent in pathComponents) builder.appendPath(pathComponent)

        if (queryParameters != null) {
            for ((queryParameterKey, queryParameterValue) in queryParameters)
                builder.appendQueryParameter(
                    queryParameterKey,
                    queryParameterValue.toString()
                )
        }

        val url = URL(builder.build().toString())
        with(url.openConnection() as HttpURLConnection) {

            requestMethod = method.toString()
            setRequestProperty("Accept", "application/json")

            if (method == HttpMethod.POST || method == HttpMethod.PUT) {
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            return this
        }
    }

    private inline fun <reified T : Any> get(
        pathComponents: Array<String>,
        noinline callback: ResponseBlock<T>?,
        queryParameters: Map<String, Any>? = null
    ) {
        thread(start = true) {
            val urlRequest = createUrlRequest(HttpMethod.GET, pathComponents, queryParameters)
            parseResponse(urlRequest, callback)
        }
    }


    private inline fun <reified T : Any> post(
        pathComponents: Array<String>, item: Any, noinline callback: ResponseBlock<T>?
    ) {
        thread(start = true) {
            val urlRequest = createUrlRequest(HttpMethod.POST, pathComponents)
            urlRequest.apply {
                writeBody(item)
            }.let { httpConnection ->
                parseResponse(httpConnection, callback)
            }
        }
    }

    private inline fun <reified T : Any> put(
        pathComponents: Array<String>, item: Any, noinline callback: ResponseBlock<T>?
    ) {
        thread(start = true) {
            val urlRequest = createUrlRequest(HttpMethod.PUT, pathComponents)
            urlRequest.apply {
                writeBody(item)
            }.let { httpConnection ->
                parseResponse(httpConnection, callback)
            }
        }
    }

    private inline fun <reified T : Any> parseResponse(
        httpConnection: HttpURLConnection,
        noinline callback: ResponseBlock<T>?
    ) {
        with(httpConnection) {
            val parsedResult = try {
                if (this.responseCode in 200..299) {
                    Triple(
                        this.responseCode, jacksonObjectMapper().readValue(
                            this.inputStream.bufferedReader().readText(),
                            T::class.java
                        ), null
                    )
                } else {
                    val errorResponse = this.errorStream.bufferedReader().readText()
                    Triple(this.responseCode, null, Throwable(errorResponse))
                }
            } catch (e: Exception) {
                Triple(0, null, e)
            }

            parsedResult.third?.let {
                Log.e(this::class.simpleName, it.message, it)
            }
            callback?.invoke(parsedResult.first, parsedResult.second, parsedResult.third)
            this.disconnect()
        }
    }

    private fun HttpURLConnection.writeBody(item: Any) {
        val outputWriter = outputStream.bufferedWriter()
        outputWriter.write(
            jacksonObjectMapper().writeValueAsString(item)
        )
        outputWriter.flush()
    }
}