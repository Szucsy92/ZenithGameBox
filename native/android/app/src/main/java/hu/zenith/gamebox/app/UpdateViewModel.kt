package hu.zenith.gamebox.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UpdateState(
    val info: UpdateInfo? = null, // elérhető frissítés (null = nincs / elhalasztva)
    val stage: Int = 0,           // 0 = felajánlva, 1 = letöltés, 2 = telepítés indítva, 3 = hiba
    val progress: Int = 0,
)

class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()
    private var apkFile: File? = null

    init {
        viewModelScope.launch {
            UpdateManager.check(getApplication())?.let { info ->
                _state.update { UpdateState(info = info, stage = 0, progress = 0) }
            }
        }
    }

    fun dismiss() { _state.update { it.copy(info = null) } }

    fun start(onReady: (File) -> Unit) {
        val info = _state.value.info ?: return
        if (_state.value.stage == 1) return
        apkFile?.takeIf { _state.value.stage == 2 && it.exists() }?.let { onReady(it); return }
        _state.update { it.copy(stage = 1, progress = 0) }
        viewModelScope.launch {
            try {
                val f = UpdateManager.download(getApplication(), info) { p -> _state.update { s -> s.copy(progress = p) } }
                apkFile = f
                _state.update { it.copy(stage = 2) }
                onReady(f)
            } catch (_: Exception) {
                _state.update { it.copy(stage = 3) }
            }
        }
    }
}
