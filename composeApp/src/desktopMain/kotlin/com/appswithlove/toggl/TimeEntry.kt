import kotlinx.serialization.Serializable

@Serializable
data class TimeEntry(
    val at: String,
    val billable: Boolean,
    val description: String?,
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
    val id: Int // project or phase id
)

@Serializable
data class TimeEntryUpdate(
    val project_id: Int,
)


@Serializable
data class TimeEntryUpdateFull(
    val id: Long,
    val project_id: Int,
    val tags: List<String>?,
    val workspace_id: Int,
    val tag_action: String = "add"
    )

