package com.manichord.viperedit.files

import android.app.Activity
import android.net.Uri
import android.text.TextUtils
import org.apache.commons.io.FileUtils
import com.manichord.viperedit.util.Device
import com.manichord.viperedit.util.GreatUri
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset

interface ISaveFileManager {

    suspend fun saveFile(uri: GreatUri, newContent: String, encoding: String): Result
}

class SaveFileManager(private val activity: Activity) : ISaveFileManager {

    override suspend fun saveFile(uri: GreatUri, newContent: String, encoding: String): Result {
        try {
            val filePath = uri.filePath
            // if the uri has no path
            if (TextUtils.isEmpty(filePath)) {
                writeUri(uri.uri!!, newContent, encoding)
            } else {
                if (Device.hasKitKatApi())
                    writeUri(uri.uri!!, newContent, encoding)
                else {
                    FileUtils.write(java.io.File(filePath),
                        newContent,
                        encoding)
                }
            }
            return Success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Failure
        }
    }

    @Throws(IOException::class)
    private fun writeUri(uri: Uri, newContent: String, encoding: String) {
        val pfd = activity.contentResolver.openFileDescriptor(uri, "w")
        val fileOutputStream = FileOutputStream(pfd!!.fileDescriptor)
        fileOutputStream.write(newContent.toByteArray(Charset.forName(encoding)))
        fileOutputStream.close()
        pfd.close()
    }
}