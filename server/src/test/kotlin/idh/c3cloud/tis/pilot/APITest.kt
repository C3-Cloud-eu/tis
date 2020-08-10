package idh.c3cloud.tis.pilot

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class APITest {
    @Autowired
    private lateinit var ehrAPI: OpenServices

    @Test
    fun getPatient() = runBlocking {
        println(ehrAPI.getPatient("123456"))
    }

}