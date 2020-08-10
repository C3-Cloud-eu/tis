package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.Patient
import idh.c3cloud.tis.PatientRepository
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.junit4.SpringRunner
import reactor.core.publisher.Mono
import reactor.util.function.*

@RunWith(SpringRunner::class)
@SpringBootTest
class PatientRepositoryTest {
    @Autowired
    lateinit var patientRepository: PatientRepository

    @Before
    fun setup() {
        patientRepository.deleteAll().block()
    }

    @Test
    fun insert() {
        patientRepository.insert(Patient("p1", "s1")).block()
        assertEquals(1, patientRepository.count().block())
        assertEquals("s1", patientRepository.findById("p1").block()?.c3cloudId)
    }

    @Test(expected = DuplicateKeyException::class)
    fun duplicateStudyId() {
        patientRepository.insert(Patient("p1", "s1")).block()
        patientRepository.insert(Patient("p2", "s1")).block()
    }

    @Test
    fun validatePaitentIds() {
        val ids = listOf("A0001", "A0002")
        val invalidIds = Mono.zip(Mono.just(ids), patientRepository.findAllById(ids).map(Patient::id).collectList())
                .map { (queried, found) -> queried - found }
                .map { it.joinToString() }
                .block()
        println(invalidIds)
    }

    @Test(expected = NoSuchElementException::class)
    fun patientNotFound() = runBlocking {
        println(patientRepository.findById("A0002").awaitSingle())
    }

}