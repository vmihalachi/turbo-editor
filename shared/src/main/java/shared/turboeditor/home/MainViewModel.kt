package shared.turboeditor.home

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import shared.turboeditor.files.Failure
import shared.turboeditor.files.IOpenFileManager
import shared.turboeditor.files.ISaveFileManager
import shared.turboeditor.files.Success
import shared.turboeditor.util.GreatUri

class MainViewModel(
        private val openFileManager: IOpenFileManager,
        private val saveFileManager: ISaveFileManager) : ViewModel() {

    private var openFileSink = MutableLiveData<OpenFileState>()

    var openFileLiveData: LiveData<OpenFileState> = openFileSink

    var greatUri: GreatUri? = GreatUri(Uri.EMPTY, "", "")
        private set

    var currentEncoding: String? = "UTF-16"
        private set

    fun openFile(newUri: GreatUri?, newFileText: String?) {
        if (newUri == null) {
            openFileSink.postValue(EmptyUriState)
            return
        }

        openFileSink.postValue(OpenFileStartState)
        viewModelScope.launch {
            when (val result = openFileManager.openFile(newUri, newFileText ?: "")) {
                is Success -> {
                    openFileSink.postValue(FileLoadedState(
                            fileName = result.fileName!!,
                            fileText = result.fileText!!
                    ))

                    greatUri = newUri
                    currentEncoding = result.encoding
                }
                Failure -> openFileSink.postValue(LoadFailedState)
            }
        }
    }

    fun saveFile() {

    }
}

sealed class OpenFileState
object EmptyUriState : OpenFileState()
object OpenFileStartState : OpenFileState()
data class FileLoadedState(
        val fileName: String,
        val fileText: String
) : OpenFileState()
object LoadFailedState : OpenFileState()