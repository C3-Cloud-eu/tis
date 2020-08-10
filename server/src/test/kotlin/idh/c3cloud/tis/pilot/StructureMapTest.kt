package idh.c3cloud.tis.pilot

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import java.io.File

@RunWith(SpringRunner::class)
@SpringBootTest
class StructureMapTest {
    @Autowired
    private lateinit var structureMap: StructureMap

    @Test
    fun mapCda() {
        val cdaFile = File(javaClass.classLoader.getResource("CDA-example.xml").path)
        val fhirBundle = runBlocking {
            structureMap.transform("provider1:cda:0.1", MediaType.TEXT_XML_VALUE, cdaFile.readText())
        }
        println(fhirBundle)
    }

}