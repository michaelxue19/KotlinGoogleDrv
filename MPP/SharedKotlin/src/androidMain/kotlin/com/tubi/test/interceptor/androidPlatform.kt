package com.tubi.test.interceptor

import android.util.Base64
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonNull.content
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

actual fun signWithSHA256(pemKey: String, message: String): String {
    return sign(message, getPrivateKeyFromString(pemKey))
}


@Throws(Exception::class)
fun sign(plainText: String, privateKey: PrivateKey): String {
    val privateSignature = Signature.getInstance("SHA256withRSA")
    privateSignature.initSign(privateKey)
    privateSignature.update(plainText.toByteArray(UTF_8))

    val signature = privateSignature.sign()

    return Base64.encodeToString(signature, Base64.DEFAULT)
}

@Throws(IOException::class, GeneralSecurityException::class)
fun getPrivateKeyFromString(key: String): RSAPrivateKey {
    val privateKeyPEM =
        key.replace("\n", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")

    val encodedPrivateKeySpec = PKCS8EncodedKeySpec(
        Base64.decode(
            privateKeyPEM,
            Base64.DEFAULT
        )
    )
    val keyFactory = KeyFactory.getInstance("RSA")
    val privateKey = keyFactory.generatePrivate(encodedPrivateKeySpec)
    return privateKey as RSAPrivateKey
}

actual fun percentEscaped(string: String): String {
    return URLEncoder.encode(string, "utf8")
}

actual fun base64URLEncodedString(string: String): String {
    val encoded = Base64.encode(
        string.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    )
    return String(encoded)
}

actual fun base64(string: String): String {
    return Base64.encodeToString(string.toByteArray(UTF_8), Base64.DEFAULT)
}

actual fun currentMillisecondsSince1970(): Long {
    return System.currentTimeMillis()
}

actual fun gzip(data: String): ByteArray {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
    return bos.toByteArray()
}
