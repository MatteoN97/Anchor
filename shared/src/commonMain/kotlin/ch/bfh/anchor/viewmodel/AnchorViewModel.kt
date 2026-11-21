package ch.bfh.anchor.viewmodel



import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import ch.bfh.anchor.data.AnchorDatabase

import ch.bfh.anchor.model.CopingTool

import ch.bfh.anchor.model.CrisisCheckIn

import ch.bfh.anchor.model.Feeling
import ch.bfh.anchor.model.MoodEntry
import ch.bfh.anchor.model.MoodScale

import ch.bfh.anchor.model.SessionQuestion

import ch.bfh.anchor.model.TherapyNote

import kotlinx.coroutines.flow.SharingStarted

import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.launch



class AnchorViewModel(db: AnchorDatabase) : ViewModel() {

    private val toolDao = db.copingToolDao()

    private val noteDao = db.therapyNoteDao()

    private val checkInDao = db.crisisCheckInDao()

    private val questionDao = db.sessionQuestionDao()
    private val moodDao = db.moodEntryDao()



    // Tools

    val tools = toolDao.getAll().stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )



    fun addTool(name: String, category: String, description: String) {

        viewModelScope.launch {

            toolDao.insert(CopingTool(name = name, category = category, description = description))

        }

    }



    fun updateTool(tool: CopingTool) {

        viewModelScope.launch { toolDao.update(tool) }

    }



    fun deleteTool(tool: CopingTool) {

        viewModelScope.launch { toolDao.delete(tool) }

    }



    // Notes

    val notes = noteDao.getAll().stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )



    fun addNote(body: String, tags: List<String>, flagged: Boolean) {

        viewModelScope.launch {

            noteDao.insert(

                TherapyNote(

                    body = body,

                    tags = tags.joinToString(","),

                    flaggedForNextSession = flagged

                )

            )

        }

    }



    fun updateNote(note: TherapyNote) {

        viewModelScope.launch { noteDao.update(note) }

    }



    fun deleteNote(note: TherapyNote) {

        viewModelScope.launch { noteDao.delete(note) }

    }



    // Crisis check-ins

    val crisisCheckIns = checkInDao.getAll().stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )



    fun addCrisisCheckIn(feeling: Feeling, feelingAfter: Int) {

        viewModelScope.launch {
            // Keep the original crisis record and its scoring unchanged.
            checkInDao.insert(CrisisCheckIn(feeling = feeling.name, feelingAfter = feelingAfter))
            // A completed post-support check-in also contributes to Mood over time.
            moodDao.insert(MoodEntry(
                score = MoodScale.fromCrisisRating(feelingAfter),
                source = MoodEntry.SOURCE_CRISIS
            ))
        }

    }



    // Shared mood timeline: manual check-ins, notifications and completed support check-ins
    val moods = moodDao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addMood(score: Int, onSaved: () -> Unit = {}) {
        require(score in 1..5)
        viewModelScope.launch {
            moodDao.insert(MoodEntry(score = score))
            onSaved()
        }
    }

    // Session questions

    val sessionQuestions = questionDao.getAll().stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )



    fun addSessionQuestion(text: String) {

        viewModelScope.launch { questionDao.insert(SessionQuestion(text = text.trim())) }

    }



    fun toggleSessionQuestion(question: SessionQuestion) {

        viewModelScope.launch { questionDao.update(question.copy(answered = !question.answered)) }

    }



    fun deleteSessionQuestion(question: SessionQuestion) {

        viewModelScope.launch { questionDao.delete(question) }

    }

}

