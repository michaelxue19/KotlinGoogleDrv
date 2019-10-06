package com.tubi.test.interceptor

import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.*
import io.ktor.client.response.readText
import io.ktor.content.ByteArrayContent
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import kotlinx.io.core.toByteArray
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class CreateResponse(val kind: String, val id: String, val name: String, val mimeType: String)

@Serializable
data class DrvFile(val id: String, val name: String, val mimeType: String)

@Serializable
data class ListResponse(val nextPageToken: String = "", val files: List<DrvFile>)

@Serializable
data class UploadResponse(
    val kind: String,
    val id: String,
    val name: String,
    val mimeType: String
)

fun generateBoundary(): String = buildString {
    repeat(32) {
        append(Random.nextInt().toString(16))
    }
}.take(70)

internal class GoogleDrvImpl : GoogleOAuth() {

    companion object {
        const val MIME_TEXT_PLAIN = "text/plain"
        const val MIME_APPLICATION_ZIP = "application/zip"
        const val MIME_APPLICATION_GZIP = "application/gzip"
        const val THRESHOLD_SIZE_FOR_ZIP = 512

        private val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private val DRIVE_UPLOAD_FILES_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private val DEFAULT_LIST_PAGE_SIZE = 50
        private val FOLDER_MIME = "application/vnd.google-apps.folder"
        private val RETURN_FIELDS = "nextPageToken, files(id, name, mimeType)"
    }

    private val httpClient = HttpClient() {
        install(JsonFeature) {

            serializer = KotlinxSerializer().apply {
                setMapper(CreateResponse::class, CreateResponse.serializer())
                setMapper(ListResponse::class, ListResponse.serializer())
                setMapper(UploadResponse::class, UploadResponse.serializer())
            }
        }
    }

    // see https://developers.google.com/drive/api/v3/reference/files/create
    suspend fun createFolder(parentFolderId: String?, folderName: String): String {

        val accessToken = prepareAccessToken()
        if (accessToken == null) {
            return ""
        }

        var folder = "root"
        parentFolderId?.let {
            folder = parentFolderId
        }

        val httpBody =
            "{\"name\": \"$folderName\",\r\n" +
                    "\"mimeType\" : \"$FOLDER_MIME\",\r\n" +
                    "\"parents\": [\r\n" +
                    "     \"$folder\"\r\n" +
                    "     ]\r\n" +
                    "}\r\n" +
                    "\r\n"

        val httpRequestBuilder = HttpRequestBuilder()
        httpRequestBuilder.method = HttpMethod.Post
        httpRequestBuilder.url(urlString = DRIVE_FILES_URL)

        httpRequestBuilder.header("Authorization", "Bearer $accessToken")

        httpRequestBuilder.body = TextContent(httpBody, contentType = ContentType.Application.Json)

        try {
            val response = httpClient.request<CreateResponse>(httpRequestBuilder)
            return response.id
        } catch (e: Exception) {
            if (e is ResponseException) {
                println(e.response.readText())
            }
        }
        return ""
    }

    suspend fun deleteFiles(cloudFolderId: String?, fileNamePattern: String) {
        println("deleteFiles")
        val cloudFiles = listFiles(cloudFolderId)
        if (cloudFiles != null) {
            for (file in cloudFiles) {
                if (fileNamePattern.toRegex().matches(file.name)) {
                    println("Delete $file.name, id=$file.id")
                    deleteFile(file.id)
                }
            }
        }
    }

    // see https://developers.google.com/drive/api/v3/reference/files/list
    /**
     * List files/directories on the current folder. if the currentFolderId
     * is null, list files in the root directory
     */
    suspend fun listFiles(currentFolderId: String?): List<DrvFile>? {
        println("listFiles")
        val accessToken = prepareAccessToken()

        if (accessToken.isEmpty()) {
            return null
        }

        var folderId = "root"

        currentFolderId?.let {
            folderId = it
        }

        val query = "'$folderId' in parents"

        var nextToken = ""

        val listFiles = mutableListOf<DrvFile>()
        do {
            val httpRequestBuilder = HttpRequestBuilder()
            httpRequestBuilder.method = HttpMethod.Get
            httpRequestBuilder.url(urlString = DRIVE_FILES_URL)

            httpRequestBuilder.header("Authorization", "Bearer $accessToken")

            httpRequestBuilder.parameter("fields", RETURN_FIELDS)
            httpRequestBuilder.parameter("q", query)
            httpRequestBuilder.parameter("pageSize", DEFAULT_LIST_PAGE_SIZE)

            if (nextToken.isNotBlank()) {
                httpRequestBuilder.parameter("pageToken", nextToken)
            }

            try {
                val response = httpClient.request<ListResponse>(httpRequestBuilder)
                listFiles.addAll(response.files)
                nextToken = response.nextPageToken
            } catch (e: Exception) {
                if (e is ResponseException) {
                    println(e.response.readText())
                }
                break
            }
        } while (nextToken.isNotBlank())


        return listFiles
    }

    // See https://developers.google.com/drive/v3/reference/files/delete
    suspend fun deleteFile(fileId: String): Boolean {
        val accessToken = prepareAccessToken()
        if (accessToken.isEmpty()) {
            return false
        }

        val httpRequestBuilder = HttpRequestBuilder()
        httpRequestBuilder.method = HttpMethod.Delete
        httpRequestBuilder.url(urlString = "$DRIVE_FILES_URL/$fileId")
        httpRequestBuilder.header("Authorization", "Bearer $accessToken")

        try {
            val response = httpClient.request<String>(httpRequestBuilder)
            return true
        } catch (e: Exception) {
            if (e is ResponseException) {
                println(e.response.readText())
            }
        }
        return false
    }

    /*
     */
    // See https://developers.google.com/drive/v3/web/manage-uploads
    suspend fun uploadFile(
        cloudFolderId: String?,
        cloudFileName: String,
        fileData: Any,
        mimeType: String
    ): String {
        var newFileId = ""
        val accessToken = prepareAccessToken()
        if (accessToken.isEmpty()) {
            return newFileId
        }

        var folderId = "root"

        if (!cloudFolderId.isNullOrBlank()) {
            folderId = cloudFolderId
        }

        val boundary = generateBoundary()
        val firstPart =
            "--$boundary\r\n" +
                    "Content-Type: application/json; charset=UTF-8\r\n" +
                    "\r\n" +
                    "{\r\n" +
                    "\"name\": \"$cloudFileName\",\r\n" +
                    "\"parents\": [\r\n" +
                    "     \"$folderId\"\r\n" +
                    "     ]\r\n" +
                    "}\r\n" +
                    "\r\n" +
                    "--$boundary\r\n" +
                    "Content-Type: $mimeType\r\n" +
                    "\r\n"

        var multiPartData = firstPart.toByteArray()

        if (fileData is ByteArray) {
            multiPartData = multiPartData.plus(fileData)
        } else if (fileData is String) {
            multiPartData = multiPartData.plus(fileData.toByteArray())
        } else {
            print("unrecognized type of file content")
            return newFileId
        }

        val endBoundary = "\r\n--$boundary--".toByteArray()
        multiPartData = multiPartData.plus(endBoundary)


        val httpRequestBuilder = HttpRequestBuilder()
        httpRequestBuilder.method = HttpMethod.Post
        httpRequestBuilder.url(urlString = DRIVE_UPLOAD_FILES_URL)
        httpRequestBuilder.header("Authorization", "Bearer $accessToken")
        httpRequestBuilder.parameter("uploadType", "multipart")
        httpRequestBuilder.body = ByteArrayContent(
            multiPartData,
            ContentType.MultiPart.Related.withParameter("boundary", boundary)
        )

        try {
            val response = httpClient.request<UploadResponse>(httpRequestBuilder)
            return response.id
        } catch (e: Exception) {
            if (e is ResponseException) {
                println(e.response.readText())
            }
        }
        return newFileId
    }

    suspend fun rename(fileId: String, newName: String): Boolean {
        val accessToken = prepareAccessToken()
        if (accessToken.isEmpty()) {
            return false
        }

        val httpBody =
            "{\"name\": \"$newName\"}\r\n\r\n"

        val httpRequestBuilder = HttpRequestBuilder()
        httpRequestBuilder.method = HttpMethod.Patch
        httpRequestBuilder.url(urlString = DRIVE_FILES_URL + "/" + fileId)
        httpRequestBuilder.header("Authorization", "Bearer $accessToken")
        httpRequestBuilder.body = TextContent(httpBody, contentType = ContentType.Application.Json)

        try {
            val response = httpClient.request<String>(httpRequestBuilder)
            return true
        } catch (e: Exception) {
            if (e is ResponseException) {
                println(e.response.readText())
            }
        }
        return false
    }
}

