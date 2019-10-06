package com.tubi.test.interceptor

//import com.soywiz.klock.DateTime
import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.response.readText
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.util.InternalAPI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class JWTHeader(val alg: String, val typ: String)

@Serializable
data class JWTClaims(
    val iss: String,
    val scope: String,
    val aud: String,
    var exp: Long,
    var iat: Long
)

@Serializable
data class AuthAssertion(
    val grant_type: String,
    var assertion: String
)

@Serializable
data class AccessToken(
    val access_token: String,
    val expires_in: Int,
    val token_type: String
)

// see https://developers.google.com/identity/protocols/OAuth2ServiceAccount
internal open class GoogleOAuth {
    private val httpClient = HttpClient() {
        install(JsonFeature) {
            serializer = KotlinxSerializer().apply {
                setMapper(AccessToken::class, AccessToken.serializer())
            }
        }
    }

    companion object {

        private val GOOGLE_OAUTH_URL = "https://oauth2.googleapis.com/token"
        private val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"
        private val HTTP_CONTENT_TYPE = "Content-Type"
        private val tokenExpirationInterval: Int = 3600
        private val expireMarginTime = 60 //seconds
    }

    private var accessToken: String = ""
    private var creationTime: Long = 0
    private var expirationTime: Long = 0

    private val header = JWTHeader("RS256", "JWT")
    private val claims = JWTClaims(
        "tubi-autotest@iron-envelope-235718.iam.gserviceaccount.com",
        "https://www.googleapis.com/auth/drive.file",
        "https://oauth2.googleapis.com/token",
        0L,
        0L
    )

    private val jsonStrtingHeader: String

    init {
        jsonStrtingHeader = Json.stringify(JWTHeader.serializer(), header)
    }

    @UseExperimental(InternalAPI::class)
    protected suspend fun prepareAccessToken(): String {
        if (!isExpired()) {
            return accessToken
        }

        this.accessToken = ""
        this.creationTime = currentTime()
        this.expirationTime = this.creationTime + GoogleOAuth.tokenExpirationInterval

        this.claims.exp = this.expirationTime
        this.claims.iat = this.creationTime

        val jsonStrtingClaim = Json.stringify(JWTClaims.serializer(), claims)

        val message =
            base64URLEncodedString(jsonStrtingHeader) + "." + base64URLEncodedString(
                jsonStrtingClaim
            )

        val signedData = signWithSHA256(pemKey, message)

        val jwtString = message + "." + (signedData)

        val response = postAuthRequest(jwtString)
        response?.let {
            this.accessToken = response.access_token
        }
        return this.accessToken
    }

    private suspend fun postAuthRequest(jwtString: String): AccessToken? {

        val assertionContent = FormDataContent(Parameters.build {
            append("grant_type", GRANT_TYPE)
            append("assertion", jwtString)
        })

        this.accessToken = ""

        val httpRequestBuilder = HttpRequestBuilder()
        httpRequestBuilder.method = HttpMethod.Post
        httpRequestBuilder.url(urlString = GOOGLE_OAUTH_URL)

        httpRequestBuilder.body = assertionContent

        try {
            val httpResponse = httpClient.request<AccessToken>(httpRequestBuilder)
            println("get authorization!")
            return httpResponse
        } catch (e: Exception) {
            if (e is ResponseException) {
                println(e.response.readText())
            }
        }

        return null
    }

    fun isExpired(): Boolean {
        val now = currentTime()
        return this.accessToken == null
                || (currentTime() > tokenExpirationInterval + creationTime - expireMarginTime)
    }

    fun currentTime(): Long {
        return currentMillisecondsSince1970() / 1000
    }

    private val pemKey =
        "add your sha256 private key"
}

