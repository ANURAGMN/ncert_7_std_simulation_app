package com.ncert7.mathandsciencelab.ui.screens.subjectscreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.mathandsciencelab.data.local.SharedPreferenceUtils
import com.ncert7.mathandsciencelab.repository.SubjectRepository
import com.ncert7.mathandsciencelab.ui.screens.subjectscreen.dataclass.SubjectScreenState
import com.ncert7.mathandsciencelab.ui.models.SubjectUiModel
import com.ncert7.mathandsciencelab.ui.theme.BrandPrimary
import com.ncert7.mathandsciencelab.utils.getLocalizedName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.map

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val repository: SubjectRepository,
    private val sharedPreferenceUtils: SharedPreferenceUtils
) : ViewModel() {

    private val _state = MutableStateFlow(SubjectScreenState())
    val state: StateFlow<SubjectScreenState> = _state.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                // Always load subjects for CLASS_LEVEL regardless of selected class
                val subjectEntities = repository.getSubjectsForClass()

                val subjectUiModels = subjectEntities.map { entity ->
                    SubjectUiModel(
                        id = entity.subjectId,
                        name = entity.getLocalizedName(),
                        color = BrandPrimary,
                        totalChapters = entity.totalChapters,
                        iconUrl = entity.iconUrl
                    )
                }

                _state.value = _state.value.copy(
                    subjects = subjectUiModels,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Sets the displayed class level in the UI without reloading data.
     * The actual data comes from CONTENT_CLASS_LEVEL, this is only for display.
     */
    fun setClassLevel(classLevel: Int) {
        _state.value = _state.value.copy(classLevel = classLevel)
    }

    fun onSubjectSelected(subjectName: String) {
        sharedPreferenceUtils.setSubjectSelection(subjectName)
    }
}