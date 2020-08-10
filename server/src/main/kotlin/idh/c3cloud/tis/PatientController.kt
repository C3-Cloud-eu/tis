package idh.c3cloud.tis

import org.springframework.dao.DuplicateKeyException
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/api/patient")
class PatientController(private val patientRepository: PatientRepository) {

    @PostMapping
    fun create(@RequestBody patient: Patient): Mono<Patient> {
        return patientRepository.insert(patient)
    }

    @GetMapping
    fun all(): Flux<Patient> {
        return patientRepository.findAll()
    }

    @DeleteMapping
    fun delete(@RequestBody patient: Patient): Mono<Void> {
        return patientRepository.delete(patient)
    }

    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicateKey(ex: DuplicateKeyException): ResponseEntity<String> {
        val msg = ex.message!!
        val error = when {
            msg.contains("index: _id_") -> "Patient ID already exists"
            msg.contains("index: c3cloudId") -> "C3-Cloud ID already exists"
            else -> msg
        }
        return ResponseEntity.badRequest().body(error)
    }

}