package ch.bfh.anchor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.bfh.anchor.data.AnchorDatabase
import ch.bfh.anchor.model.CopingTool
import ch.bfh.anchor.model.CrisisCheckIn
import ch.bfh.anchor.model.Feeling
import ch.bfh.anchor.model.MoodCheckIn
import ch.bfh.anchor.model.MoodCheckInResult
import ch.bfh.anchor.model.MoodEntry
import ch.bfh.anchor.model.MoodScale
import ch.bfh.anchor.model.SessionQuestion
import ch.bfh.anchor.model.TherapyNote
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ch.bfh.anchor.platform.nowMillis

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
                    tags = tags,
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
            // Keep original post-support record and its 1=Great rating convention intact.
            checkInDao.insert(CrisisCheckIn(feeling = feeling.name, feelingAfter = feelingAfter))
            // Also add a timeline point using the shared 5=Great convention.
            moodDao.insert(MoodEntry(
                score = MoodScale.fromCrisisRating(feelingAfter),
                source = MoodEntry.SOURCE_CRISIS
            ))
        }
    }

    // Manual, notification and post-support mood entries share this timeline.
    val moods = moodDao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Serialises taps: two quick taps must not both see "no check-in this hour" and both insert.
    private val moodCheckInLock = Mutex()

    /**
     * Records an in-app check-in, at most one per hour (see [MoodCheckIn]). Within the hour a
     * different score replaces the current one; the same score changes nothing.
     */
    fun recordMood(score: Int, onResult: (MoodCheckInResult) -> Unit = {}) {
        require(score in 1..5)
        viewModelScope.launch {
            val result = moodCheckInLock.withLock {
                val now = nowMillis()
                val current = moodDao.latest(MoodEntry.SOURCE_APP)
                    ?.takeIf { now < MoodCheckIn.nextAllowedAt(it) }
                when {
                    current == null -> MoodEntry(recordedAt = now, score = score)
                        .also { moodDao.insert(it) }
                        .let { MoodCheckInResult.Saved(it) }
                    current.score == score -> MoodCheckInResult.Unchanged(current)
                    else -> {
                        moodDao.updateScore(current.id, score)
                        MoodCheckInResult.Updated(current.copy(score = score))
                    }
                }
            }
            onResult(result)
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
