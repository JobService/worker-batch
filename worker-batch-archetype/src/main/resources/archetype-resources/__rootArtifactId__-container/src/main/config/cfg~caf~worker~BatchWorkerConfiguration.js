({
    workerName: "${rootArtifactId}",
    workerVersion: "${project.version}",
    outputQueue: getenv("CAF_BATCH_WORKER_ERROR_QUEUE")
            || getenv("CAF_WORKER_OUTPUT_QUEUE")
            || (getenv("CAF_WORKER_BASE_QUEUE_NAME") || getenv("CAF_WORKER_NAME") || "worker") + "-out",
    threads: getenv("CAF_WORKER_THREADS") || 1,
    cacheExpireTime: getenv("CAF_BATCH_WORKER_CACHE_EXPIRE_TIME") || 120,
    returnValueBehaviour: getenv("CAF_BATCH_WORKER_ERROR_QUEUE") ? "RETURN_NONE" : undefined
});
