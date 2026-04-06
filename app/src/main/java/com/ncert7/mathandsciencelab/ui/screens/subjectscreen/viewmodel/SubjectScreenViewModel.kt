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
                val subjectEntities = repository.getSubjectsForClass(_state.value.classLevel)


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

    fun setClassLevel(classLevel: Int) {
        _state.value = _state.value.copy(classLevel = classLevel)
        loadSubjects()
    }

    fun onSubjectSelected(subjectId: String) {
        sharedPreferenceUtils.setSubjectSelection(subjectId)
    }
}