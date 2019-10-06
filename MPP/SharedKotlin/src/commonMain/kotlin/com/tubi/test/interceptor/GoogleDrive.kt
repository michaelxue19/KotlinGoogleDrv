package com.tubi.test.interceptor

import kotlinx.io.ByteArrayOutputStream

class GoogleDrive {
    companion object {
        private const val TAG = "GoogleDrvApi"
    }

    private val googleDrvImpl = GoogleDrvImpl()

    /**
     * Create a directory on the google drive.
     */
    suspend fun createDir(app: Any?, cloudFolder: String): String {
        return createOneDir(null, cloudFolder)
    }

    /*
     * create a new directory on the google drive, if the folderName was existing already,
     * delete it firstly
     */
    suspend protected fun createOneDir(parentFolderId: String?, newFolderName: String): String {

        var newFolderId = ""
        googleDrvImpl.deleteFiles(parentFolderId, newFolderName)
        newFolderId = googleDrvImpl.createFolder(parentFolderId, newFolderName)

        return newFolderId
    }

    suspend fun uploadToCloud(
        folderId: String?, content: String,
        cloudFileName: String
    ): String {
        var uploadFileId = ""
        if (content.length > GoogleDrvImpl.THRESHOLD_SIZE_FOR_ZIP) {
            val gzipContent = gzip(content)
            uploadFileId = googleDrvImpl.uploadFile(
                folderId, cloudFileName, gzipContent,
                GoogleDrvImpl.MIME_APPLICATION_GZIP
            )

        }
        if (uploadFileId.isBlank()) {
            uploadFileId = googleDrvImpl.uploadFile(
                folderId, cloudFileName, content,
                GoogleDrvImpl.MIME_TEXT_PLAIN
            )
        }

        return uploadFileId
    }

    suspend fun renameCloudFile(fileId: String, newName: String): Boolean {
        return googleDrvImpl.rename(fileId, newName)
    }

    fun gzip(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        //GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
        return bos.toByteArray()
    }
}
