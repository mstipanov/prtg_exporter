package org.infobip.prometheus.prtgexporter

import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Classes that extends this abstract class are used in processing data, runs in separates thread and supports graceful
 * shutdown.
 */
abstract class AbstractProcessor : Runnable {

    /**
     * Is processor started.
     */
    private val started = AtomicBoolean(false)

    /**
     * Processing condition.
     */
    @Volatile protected var thread: Thread? = null

    /**
     * Thread name.
     */
    var threadName = this.javaClass.simpleName + "-Thread"

    /**
     * Returns is processor started.
     *
     * @return processor status.
     */
    val isStarted: Boolean
        get() = started.get()

    protected val isProcessing: Boolean
        get() = thread != null

    @PostConstruct
    @Synchronized
    fun start() {
        if (started.get()) {
            return
        }

        thread = Thread(this)
        thread!!.name = threadName + "-" + thread!!.id

        started.set(true)
        thread!!.start()
    }

    override fun run() {
        val thisThread = Thread.currentThread()

        try {
            while (thread === thisThread) {
                process()
            }
        } catch (e: Exception) {
            LOGGER.error("Thread " + thisThread.name + " exception", e)
        }

        started.set(false)
    }

    /**
     * Abstract method on which implementation depends what kind of processing will be done.
     */
    protected abstract fun process()

    private fun stopProcessing(): Thread? {
        val l_thread = thread
        thread = null
        return l_thread
    }

    /**
     * Gracefully shuts down processor.
     */
    @PreDestroy
    fun stop() {
        stop(DEFAULT_STOP_TIMEOUT)
    }

    /**
     * Waits at most millis milliseconds for thread to stop. A timeout of 0 means to wait forever
     *
     * @param stopTimeout the time to wait in milliseconds.
     * @return boolean indicating is thread stopped.
     */
    @Synchronized
    fun stop(stopTimeout: Int): Boolean {
        if (!isStarted) {
            return true
        }

        val l_thread = stopProcessing() ?: return false

        interruptSafely(l_thread)

        if (Thread.currentThread() === l_thread) {
            return false
        }

        val attempts = 3
        var millis = stopTimeout
        if (millis >= attempts) {
            millis /= attempts
        }

        for (i in 0 until attempts) {
            try {
                l_thread.join(millis.toLong())
            } catch (e: Exception) {
                //ignore
            }

            if (!isStarted) {
                break
            }

            interruptSafely(l_thread)
        }

        if (isStarted) {
            LOGGER.error("Thread " + l_thread.name + " FAILED to stop.")
            return false
        }

        return true
    }

    private fun interruptSafely(l_thread: Thread) {
        try {
            l_thread.interrupt()
        } catch (e: Exception) {
            LOGGER.error("Thread " + l_thread.name + " FAILED to interrupt.", e)
        }

    }

    companion object {

        val DEFAULT_STOP_TIMEOUT = 3000
        private val LOGGER = LoggerFactory.getLogger(AbstractProcessor::class.java)
    }

}
