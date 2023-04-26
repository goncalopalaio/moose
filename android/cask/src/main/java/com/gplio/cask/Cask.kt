package com.gplio.cask

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

private const val VERBOSE = false
private const val HEADER_SIZE = 3 * Int.SIZE_BYTES + 2 * Short.SIZE_BYTES

/**
 * Really basic and fragile version of Bitcask.
 * http://highscalability.com/blog/2011/1/10/riaks-bitcask-a-log-structured-hash-table-for-fast-keyvalue.html
 * https://riak.com/assets/bitcask-intro.pdf
 */
class Cask(private val caskPath: String) {
    // TODO (Gonçalo): Handle concurrent updates
    // TODO (Gonçalo): Is it worth to use FileChannel.map to map the file to memory completely? for small files it shouldn't
    // TODO (Gonçalo): Check FileChannel.lock

    private data class Entry(
        val epoch: Int,
        val keySize: Int,
        val valueSize: Int,
        val keyType: Short,
        val valueType: Short,
        val key: String,
        val value: String,
    )

    private var endOfFile: Long = 0
    private val keyDirectory: HashMap<String, Long> = HashMap()

    /**
     * Initializes internal indexes.
     * This is made explicit so we have finer control:
     * - When key offsets are loaded from disk
     * - Merging of old entries is made
     */
    fun init() {
        log("init - HEADER_SIZE=$HEADER_SIZE")
        // Merge - compact older entries
        // Update endOfFile
        // Update in-memory key offsets
        // TODO (Gonçalo): Is it really worth it to have this be explicit. This opens the possibility for a read/write to happen without full scanning of the previous key/values to have happened

        merge()
        index()
    }

    fun read(key: String): String? {
        val offsetOfEntry = keyDirectory[key]
        log("read - key=$key, offsetOfEntry=$offsetOfEntry")
        if (offsetOfEntry == null) return null

        val file = File(caskPath)
        val fis = FileInputStream(file)
        fis.use {
            val channel = it.channel
            channel.position(offsetOfEntry)

            val size = channel.size()
            log("read - offsetOfEntry=${offsetOfEntry.toInt()}, size=$size")

            val entry = readEntry(channel)

            // TODO (Gonçalo): check key matches
            return entry.value
        }
    }

    fun add(key: String, value: String) {
        val entry = createEntry(key.toByteArray(), value.toByteArray())

        if (entry == null) {
            log("add - entry is null | key=$key, value=$value")
            return
        }

        appendToFile(caskPath) {
            val channel = it.channel

            val initialPosition =
                channel.position() // TODO (Gonçalo): Catch IOException; atomic updates of endOfFile
            val bytesWritten = channel.write(entry)
            log("add - key=$key, value=$value, initialPosition=$initialPosition, bytesWritten=$bytesWritten")

            if (bytesWritten == 0) return@appendToFile
            channel.force(false)

            endOfFile = initialPosition + bytesWritten
            keyDirectory[key] = initialPosition
            log("add - key=$key, initialPosition=$initialPosition, endOfFile=$endOfFile")
        }
    }

    private fun merge() {
        // TODO (Gonçalo): Merge and remove older values.
    }

    private fun index() {
        // TODO (Gonçalo): Save key directory in a file?
        val file = File(caskPath)
        if (!file.exists()) {
            log("index - No file")
            keyDirectory.clear()
            return
        }

        val fis = FileInputStream(file)
        fis.use {
            val channel = it.channel
            var position = channel.position()
            val end = channel.size() // TODO (Gonçalo): Catch IOException
            do {
                val beforePosition = position
                val entry = readEntry(channel)
                val afterPosition = channel.position() // TODO (Gonçalo): Catch IOException
                log("index - beforePosition=$beforePosition, afterPosition=$afterPosition, entry=$entry")
                position = afterPosition

                // Note: The key entries should be in order.
                // Fragile: There are several methods that throw, so there's the chance that keyDirectory is left pointing to an older value if something throws and we manage to continue.
                log("index - key=${entry.key}, at=$beforePosition")
                keyDirectory[entry.key] = beforePosition

            } while (position < end)
        }
    }

    private fun readEntry(channel: FileChannel): Entry {
        val headerBuffer = ByteBuffer.allocate(HEADER_SIZE)
        val header = arrayOf(headerBuffer)
        val readHeaderBytes = channel.read(header, 0, 1)
        log("read - readHeaderBytes=$readHeaderBytes, headerBuffer=$headerBuffer")

        // TODO (Gonçalo): Check if header was completely read

        headerBuffer.flip()
        val epoch = headerBuffer.int
        val keySize = headerBuffer.int
        val valueSize = headerBuffer.int
        val keyType = headerBuffer.short
        val valueType = headerBuffer.short

        log("read - epoch=$epoch, keySize=$keySize, valueSize=$valueSize, keyType=$keyType, valueType=$valueType")

        val bodyBuffer = ByteBuffer.allocate(keySize + valueSize)
        val body = arrayOf(bodyBuffer)
        val readBodyBytes = channel.read(body, 0, 1)
        log("read - readBodyBytes=$readBodyBytes, bodyBuffer=$bodyBuffer")

        // TODO (Gonçalo): Check if body was completely read

        bodyBuffer.flip()
        val bodyByteArray = bodyBuffer.array()
        val readKey = String(bodyByteArray, 0, keySize)
        val readValue = String(bodyByteArray, keySize, valueSize)
        log("read - readKey=$readKey, readValue=$readValue")

        return Entry(
            epoch = epoch,
            keySize = keySize,
            valueSize = valueSize,
            keyType = keyType,
            valueType = valueType,
            key = readKey,
            value = readValue
        )
    }

    private fun appendToFile(
        path: String,
        predicate: (fileOutputStream: FileOutputStream) -> Unit
    ) {
        val file = File(path)
        val append = true
        val fos = FileOutputStream(file, append)
        fos.use(predicate) // TODO (Gonçalo): Catch Throwable
    }

    private fun createEntry(key: ByteArray, value: ByteArray): ByteBuffer? {

        val epoch: Int = System.currentTimeMillis().toInt()
        val keySize: Int = key.size
        val valueSize: Int = value.size
        val keyType: Short = 1
        val valueType: Short = 2

        val entryNoCrc = with(ByteBuffer.allocate(HEADER_SIZE + key.size + value.size)) {
            putInt(epoch)
            putInt(keySize)
            putInt(valueSize)
            putShort(keyType)
            putShort(valueType)
            put(key)
            put(value)
        }

        log("createEntry - epoch=$epoch, keySize=$keySize, valueSize=$valueSize, keyType=$keyType, valueType=$valueType, key=$key, value=$value")
        return entryNoCrc.flip()
    }

    private fun log(text: String) {
        if (!VERBOSE) return
        println(text) // TODO (Gonçalo): Inject real logger
    }
}