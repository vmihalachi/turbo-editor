package shared.turboeditor.files

import android.app.Activity
import android.net.Uri
import android.text.TextUtils
import com.spazedog.lib.rootfw4.RootFW
import com.spazedog.lib.rootfw4.Shell
import org.apache.commons.io.FileUtils
import shared.turboeditor.util.Device
import shared.turboeditor.util.GreatUri
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset

interface ISaveFileManager {

    suspend fun saveFile(uri: GreatUri, newContent: String, encoding: String): Result
}

class SaveFileManager(private val activity: Activity) : ISaveFileManager {

    override suspend fun saveFile(uri: GreatUri, newContent: String, encoding: String): Result {
        var isRootNeeded = false
        var resultRoot: Shell.Result? = null

        try {
            val filePath = uri.filePath
            // if the uri has no path
            if (TextUtils.isEmpty(filePath)) {
                writeUri(uri.uri!!, newContent, encoding)
            } else {
                isRootNeeded = !uri.isWritable
                if (!isRootNeeded) {
                    if (Device.hasKitKatApi())
                        writeUri(uri.uri!!, newContent, encoding)
                    else {
                        FileUtils.write(java.io.File(filePath),
                                newContent,
                                encoding)
                    }
                } else {

                    if (RootFW.connect()!!) {
                        val systemPart = RootFW.getDisk(uri.parentFolder)
                        systemPart.mount(arrayOf("rw"))

                        val file = RootFW.getFile(uri.filePath)
                        resultRoot = file.writeResult(newContent)

                        RootFW.disconnect()
                    }

                }// if we can read the file associated with the uri

            }

            return if (isRootNeeded) {
                if (resultRoot != null && resultRoot.wasSuccessful()!!) {
                    Success()
                } else if (resultRoot != null) {
                    //                    message = negativeMessage + " command number: " + resultRoot.commandNumber + " result code: " + resultRoot.resultCode + " error lines: " + resultRoot.string
                    Failure
                } else
                    Failure
            } else
                Success()
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