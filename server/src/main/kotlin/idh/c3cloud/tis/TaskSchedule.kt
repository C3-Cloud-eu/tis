package idh.c3cloud.tis

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * A TaskSchedule defines a schedule of a task. The TaskSchedule will be marshalled into JSON
 * (by Jackson) and saved in a database.
 */
@Document(collection = "TaskSchedule")
data class TaskSchedule(val task: String,
                        val parameters: Map<String, String>,
                        val start: Instant = Instant.now(),
                        val repeat: RepeatOption,
                        val until: Instant? = null,
                        val id: ObjectId = ObjectId(),
                        val created: Instant = Instant.now()) {

    enum class RepeatOption {
        `none`, `1-hour`, `1-day`, `1-week`, `1-month`
    }

    enum class State {
        Active, Stopped, Complete
    }

    var lastExecution: Instant? = null
    var executionCount: Int = 0
    var state: State = State.Active
}
