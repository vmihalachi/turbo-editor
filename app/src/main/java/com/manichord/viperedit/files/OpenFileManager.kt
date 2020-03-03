package com.manichord.viperedit.files

import android.app.Activity
import android.net.Uri
import android.text.TextUtils
import com.spazedog.lib.rootfw4.RootFW
import org.apache.commons.io.FilenameUtils
import com.manichord.viperedit.home.texteditor.FileUtils
import com.manichord.viperedit.preferences.PreferenceHelper
import com.manichord.viperedit.util.GreatUri
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

interface IOpenFileManager {

    suspend fun openFile(newUri: GreatUri, newFileText: String): Result
}

class OpenFileManager(private val activity: Activity) : IOpenFileManager {

    private lateinit var fileText: String

    private lateinit var fileName: String

    private lateinit var encoding: String

    private var isRootRequired: Boolean = false

    override suspend fun openFile(newUri: GreatUri, newFileText: String): Result {
        try {
            val fileExtension: String

            // if no new uri
            if (newUri.uri == null || newUri.uri === Uri.EMPTY) {
                fileExtension = "txt"
                fileText = newFileText
            } else {
                val filePath = newUri.filePath!!

                // if the uri has no path
                if (TextUtils.isEmpty(filePath)) {
                    fileName = newUri.fileName!!
                    fileExtension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.getDefault())

                    readUri(newUri.uri!!, filePath, false)
                } else {
                    fileName = FilenameUtils.getName(filePath)
                    fileExtension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.getDefault())

                    isRootRequired = !newUri.isReadable
                    // if we cannot read the file, root permission required
                    if (isRootRequired) {
                        readUri(newUri.uri!!, filePath, true)
                    } else {
                        readUri(newUri.uri!!, filePath, false)
                    }// if we can read the file associated with the uri
                }// if the uri has a path
            }

            return Success(fileText = fileText, fileName = fileName, fileExtension = fileExtension, encoding = encoding)
        } catch (e: Exception) {
            fileText = ""
            return Failure
        }
    }

    @Throws(IOException::class)
    private fun readUri(uri: Uri, path: String, asRoot: Boolean) {
        var buffer: BufferedReader? = null
        val stringBuilder = StringBuilder()
        var line: String?

        if (asRoot) {

            encoding = "UTF-8"

            // Connect the shared connection
            if (RootFW.connect()!!) {
                val reader = RootFW.getFileReader(path)
                buffer = BufferedReader(reader)
            }
        } else {

            val autoencoding = PreferenceHelper.getAutoEncoding(activity)
            if (autoencoding) {
                encoding = FileUtils.getDetectedEncoding(activity.contentResolver.openInputStream(uri)!!)
                if (encoding.isEmpty()) {
                    encoding = PreferenceHelper.getEncoding(activity)
                }
            } else {
                encoding = PreferenceHelper.getEncoding(activity)
            }

            val inputStream = activity.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                buffer = BufferedReader(InputStreamReader(inputStream, encoding))
            }
        }

        if (buffer != null) {
            line = buffer.readLine()
            while (line != null) {
                stringBuilder.append(line)
                stringBuilder.append("\n")
                line = buffer.readLine()
            }
            buffer.close()
            fileText = stringBuilder.toString()
        }

        if (isRootRequired)
            RootFW.disconnect()
    }
}