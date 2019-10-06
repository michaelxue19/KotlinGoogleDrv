package com.tubi.test.interceptor

import com.tubi.test.interceptor.GoogleDrvImpl.Companion.MIME_APPLICATION_GZIP
import com.tubi.test.interceptor.GoogleDrvImpl.Companion.MIME_TEXT_PLAIN
import com.tubi.test.interceptor.GoogleDrvImpl.Companion.THRESHOLD_SIZE_FOR_ZIP
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class GoogleDrvApi {
    companion object {
        private const val TAG = "GoogleDrvApi"
    }

    private val googleDrvImpl = GoogleDrvImpl()

    /**
     * Create a directory on the google drive.
     */
    fun createDir(app: Any?, cloudFolder: String): String {
        return createOneDir(null, cloudFolder)
    }

    /*
     * create a new directory on the google drive, if the folderName was existing already,
     * delete it firstly
     */
    protected fun createOneDir(parentFolderId: String?, newFolderName: String): String {

        var newFolderId = ""
        runBlocking {

            try {
                val deleteJob = async { googleDrvImpl.deleteFiles(parentFolderId, newFolderName) }
                deleteJob.await()

                val createJob = async { googleDrvImpl.createFolder(parentFolderId, newFolderName) }
                newFolderId = createJob.await()
            } catch (e: Exception) {
                // rethrow
                throw CoruntineException("Error in createOneDir", e)
            } catch (e: Error) {
                throw CoruntineException("Error in createOneDir", e)
            }
        }

        return newFolderId
    }

    fun uploadToCloud(
        folderId: String?, content: String,
        cloudFileName: String
    ): String {
        var newFileId = ""
        runBlocking {
            try {
                val uploadTask = async {
                    var uploadFileId = ""
                    if (content.length > THRESHOLD_SIZE_FOR_ZIP) {
                        val gzipContent = gzip(content)
                        uploadFileId = googleDrvImpl.uploadFile(
                            folderId,
                            cloudFileName,
                            gzipContent,
                            MIME_APPLICATION_GZIP
                        )

                    }
                    if (uploadFileId.isBlank()) {
                        uploadFileId =
                            googleDrvImpl.uploadFile(
                                folderId,
                                cloudFileName,
                                content,
                                MIME_TEXT_PLAIN
                            )
                    }
                    uploadFileId
                }
                newFileId = uploadTask.await()
            } catch (e: Exception) {
                // rethrow
                throw CoruntineException("Error in uploadToCloud", e)
            } catch (e: Error) {
                throw CoruntineException("Error in createOneDir", e)
            }
        }
        return newFileId
    }

    fun renameCloudFile(fileId: String, newName: String): Boolean {
        var success = false
        runBlocking {
            try {
                val renameTask = async {
                    googleDrvImpl.rename(fileId, newName)
                }
                success = renameTask.await()
            } catch (e: Exception) {
                // rethrow
                throw CoruntineException("Error in renameCloudFile", e)
            } catch (e: Error) {
                throw CoruntineException("Error in createOneDir", e)
            }
        }
        return success
    }
}
