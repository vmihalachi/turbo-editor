package shared.turboeditor.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import shared.turboeditor.files.IOpenFileManager
import shared.turboeditor.files.ISaveFileManager
import shared.turboeditor.util.GreatUri

class MainViewModel(
        private val openFileManager: IOpenFileManager,
        private val saveFileManager: ISaveFileManager) : ViewModel() {

    var greatUri: GreatUri? = GreatUri(Uri.EMPTY, "", "")
        private set

    var currentEncoding: String? = "UTF-16"
        private set

    fun openFile() {

    }

    fun saveFile() {

    }
}