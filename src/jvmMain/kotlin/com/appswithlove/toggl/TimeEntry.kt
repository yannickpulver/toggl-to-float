import kotlinx.serialization.Serializable

@Serializable
data class TimeEntry(
    val at: String,
    val billable: Boolean,
    val description: String,
    val duration: Int,
    val duronly: Boolean,
    val id: Long,
    val project_id: Int?,
    val start: String,
    val stop: String?,
    val tag_ids: List<Int>?,
    val tags: List<String>?,
    val uid: Int,
    val user_id: Int,
    val wid: Int,
    val workspace_id: Int
)


data class TimeEntryForPublishing(
    val timeEntry: TimeEntry,
    val projectId: Int,
    val phaseId: Int? = null,
)

