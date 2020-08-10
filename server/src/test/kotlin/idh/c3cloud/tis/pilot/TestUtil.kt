package idh.c3cloud.tis.pilot

import java.io.File
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

object TestUtil {

    fun getFile(name: String) = File(javaClass.classLoader.getResource(name).path)

    fun getProperty(source: Any, name: String) =
            source.javaClass.kotlin.memberProperties.find { it.name == name }?.apply {isAccessible = true}?.get(source)

}