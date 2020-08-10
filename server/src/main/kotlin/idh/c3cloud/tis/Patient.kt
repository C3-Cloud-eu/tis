package idh.c3cloud.tis

import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate
import java.time.Instant

@Document(collection = "Patient")
data class Patient(
        val id: String,
        @Indexed(unique = true)
        val c3cloudId: String,
        var email: String? = null,
        var layer3: Boolean = false,
        var useMedicalDevice: Boolean = false,
        var dateCreated: LocalDate = LocalDate.now(),
        var lastImport: Instant? = null
)
