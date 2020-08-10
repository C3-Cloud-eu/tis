package idh.c3cloud.tis

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.coroutines.Job
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.annotation.Transient
import java.time.Instant

@Document(collection = "TaskExecution")
data class TaskExecution(
            val task: String,
            val startTime: Instant = Instant.now(),
            val id: ObjectId = ObjectId()) {

    enum class State {
        Running, Success, Cancelled, Error
    }
    var state: State = State.Running

    var endTime: Instant? = null

    var details = emptyMap<String, Any>()

    @JsonIgnore
    @Transient
    var job: Job? = null

}