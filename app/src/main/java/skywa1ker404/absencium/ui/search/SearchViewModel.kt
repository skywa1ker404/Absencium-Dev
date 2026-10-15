package skywa1ker404.absencium.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import skywa1ker404.absencium.AppContainer
import skywa1ker404.absencium.data.db.GuideEntity
import skywa1ker404.absencium.data.db.NoteEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The notes and guides the global search looks through, kept live from Room. */
data class SearchIndex(
    val notes: List<NoteEntity> = emptyList(),
    val guides: List<GuideEntity> = emptyList(),
)

class SearchViewModel(container: AppContainer) : ViewModel() {

    val index: StateFlow<SearchIndex> = combine(
        container.database.noteDao().observeAll(),
        container.database.guideDao().observeAll(),
    ) { notes, guides -> SearchIndex(notes, guides) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchIndex())
}
