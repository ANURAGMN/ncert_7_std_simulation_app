package com.anurag.eduapp.ui.screens.subjectscreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anurag.eduapp.data.local.SharedPreferenceUtils
import com.anurag.eduapp.repository.SubjectRepository
import com.anurag.eduapp.ui.screens.subjectscreen.dataclass.SubjectScreenState
import com.anurag.eduapp.ui.models.SubjectUiModel
import com.anurag.eduapp.ui.theme.BrandPrimary
import com.anurag.eduapp.utils.getLocalizedName
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
                        totalChapters = entity.totalChapters
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