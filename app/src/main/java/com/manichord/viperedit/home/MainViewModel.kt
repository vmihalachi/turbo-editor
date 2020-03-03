package com.manichord.viperedit.home

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.manichord.viperedit.files.Failure
import com.manichord.viperedit.files.IOpenFileManager
import com.manichord.viperedit.files.ISaveFileManager
import com.manichord.viperedit.files.Success
import com.manichord.viperedit.util.GreatUri

class MainViewModel(
        private val openFileManager: IOpenFileManager,
        private val saveFileManager: ISaveFileManager) : ViewModel() {

    private var openFileSink = MutableLiveData<OpenFileState>()
    private var saveFileSink = MutableLiveData<SaveFileState>()

    var openFileLiveData: LiveData<OpenFileState> = openFileSink

    var saveFileLiveData: LiveData<SaveFileState> = saveFileSink

    var greatUri: GreatUri? = GreatUri(Uri.EMPTY, "", "")
        private set

    var currentEncoding: String? = "UTF-16"
        private set

    fun openFile(newUri: GreatUri?, newFileText: String?) {
        if (newUri == null) {
            openFileSink.postValue(OpenFileState.EmptyUriState)
            return
        }

        openFileSink.postValue(OpenFileState.OpenFileStartState)
        viewModelScope.launch {
            when (val result = openFileManager.openFile(newUri, newFileText ?: "")) {
                is Success -> {
                    openFileSink.postValue(OpenFileState.FileLoadedState(
                            fileName = result.fileName!!,
                            fileText = result.fileText!!
                    ))

                    greatUri = newUri
                    currentEncoding = result.encoding
                }
                Failure -> openFileSink.postValue(OpenFileState.LoadFailedState)
            }
        }
    }

    fun saveFile(uri: GreatUri, text: String, encoding: String) {
        viewModelScope.launch {
            when (saveFileManager.saveFile(uri, text, encoding)) {
                is Success -> saveFileSink.postValue(SaveFileState.Success(uri.fileName ?: ""))
                Failure -> saveFileSink.postValue(SaveFileState.Failed)
            }
        }
    }

    fun saveFileAndOpen(uri: GreatUri, text: String, encoding: String) {
        viewModelScope.launch {
            when (saveFileManager.saveFile(uri, text, encoding)) {
                is Success -> saveFileSink.postValue(SaveFileState.SuccessAndOpen(uri.fileName ?: ""))
                Failure -> saveFileSink.postValue(SaveFileState.Failed)
            }
        }
    }
}

sealed class OpenFileState {
    object EmptyUriState : OpenFileState()
    object OpenFileStartState : OpenFileState()
    data class FileLoadedState(
            val fileName: String,
            val fileText: String
    ) : OpenFileState()

    object LoadFailedState : OpenFileState()
}

sealed class SaveFileState {
    data class Success(val fileName: String) : SaveFileState()
    data class SuccessAndOpen(val fileName: String) : SaveFileState()
    object Failed : SaveFileState()
}