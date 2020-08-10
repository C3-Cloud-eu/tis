package idh.c3cloud.tis

import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

/**
 * A task could be an import or export ETL task, a query, or even an analytics/ML task in future.
 * A task will be marshalled into JSON (by Jackson) and saved in a database.
 *
 * The parameters are a list of names, e.g. patient, which will be interpreted by the web layer
 * and individual task logic.
 */
@Document(collection = "Task")
data class Task(val type: Type,
                val name: String,
                val version: Int = 1,
                val id: String = name + "-v" + version,
                val dateModified: LocalDate = LocalDate.now(),
                val description: String = "",
                val parameters: Map<String, String> = emptyMap()) {

    enum class Type { Import, Export }

}
