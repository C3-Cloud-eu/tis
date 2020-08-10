package idh.c3cloud.tis

interface TaskExecutable {
    /**
     *
     * @return A key-value map to be saved in the execution log
     * @throws TaskExecutionException
     */
    operator suspend fun invoke(parameters: Map<String, String>): Map<String, Any>

}

