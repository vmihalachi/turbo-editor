package shared.turboeditor.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import shared.turboeditor.files.ISaveFileManager
import shared.turboeditor.files.OpenFileManager

class MainViewModelFactory(
        private val openFileManager: OpenFileManager,
        private val saveFileManager: ISaveFileManager) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(openFileManager, saveFileManager) as T
    }
}