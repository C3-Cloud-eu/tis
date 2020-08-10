package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.Task

object TestData {

    fun helloWorld(): Task {
        return Task(name = "HelloWorld",
                description = "Test task",
                type = Task.Type.Import,
                parameters = mapOf("patient" to ""))
    }

    fun provider1(): Task {
        return Task(name = "Provider1-Import",
                description = "Import data by patient id from CDA and DBP services into FHIR repository",
                type = Task.Type.Import,
                parameters = mapOf("patient" to ""))
    }

    fun provider2(): Task {
        return Task(name = "Provider1-Import",
                description = "Import data by patient id from API into FHIR repository",
                type = Task.Type.Import,
                parameters = mapOf("patient" to ""))
    }

    fun provider3(): Task {
        return Task(name = "Provider3-Import",
                description = "Import data from CSV files into FHIR repository",
                type = Task.Type.Import,
                parameters = mapOf(
                        PARAM_GP_EMIS_FILE to "file:///C:/c3cloud/test/EHR.csv",
                        PARAM_LORENZO_COMMUNITY_FILE to "file:///C:/c3cloud/test/GP.csv",
                        "patient" to ""
                ))
    }

    fun provider3Id(): Task {
        return Task(name = "Provider3-Import-By-Id",
                description = "Import data by patient id from CSV files into FHIR repository",
                type = Task.Type.Import,
                parameters = mapOf(
                        PARAM_GP_EMIS_FILE to "file:///C:/c3cloud/test/EHR.csv",
                        PARAM_LORENZO_COMMUNITY_FILE to "file:///C:/c3cloud/test/GP.csv",
                        "patient" to ""))
    }

}