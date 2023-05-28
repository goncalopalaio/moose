package com.gplio.cask

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

private const val VERBOSE = false
private const val HEADER_SIZE = 3 * Int.SIZE_BYTES + 2 * Short.SIZE_BYTES
private const val MERGE_FILE_EXTENSION = ".merge"

/**
 * Really basic and fragile version of Bitcask.
 * http://highscalability.com/blog/2011/1/10/riaks-bitcask-a-log-structured-hash-table-for-fast-keyvalue.html
 * https://riak.com/assets/bitcask-intro.pdf
 * https://docs.riak.com/riak/kv/2.2.3/setup/planning/backend/bitcask/index.html
 */
class Cask(private val caskPath: String) {

    constructor(file: File) : this(file.path)

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

        val file = File(caskPath)
        if (!file.exists()) {
            log("init - No file")
            keyDirectory.clear()
            return
        }

        index(file) { entry, startPosition, _ ->
            keyDirectory[entry.key] = startPosition // TODO - Not currently thread safe
        }
        merge()
    }

    fun read(key: String): String? {
        val offsetOfEntry = keyDirectory[key]
        if (VERBOSE) log("read - key=$key, offsetOfEntry=$offsetOfEntry")
        if (offsetOfEntry == null) return null

        val file = File(caskPath)
        val fis = FileInputStream(file)
        fis.use {
            val channel = it.channel
            channel.position(offsetOfEntry)

            val size = channel.size()
            if (VERBOSE) log("read - offsetOfEntry=${offsetOfEntry.toInt()}, size=$size")

            val entry = readEntry(channel)

            // TODO (Gonçalo): check key matches
            return entry.value
        }
    }

    fun add(key: String, value: String) {
        val entry = createEntry(key.toByteArray(), value.toByteArray())

        if (entry == null) {
            if (VERBOSE) log("add - entry is null | key=$key, value=$value")
            return
        }

        val file = File(caskPath)
        appendToFile(file) {
            val channel = it.channel

            val initialPosition =
                channel.position() // TODO (Gonçalo): Handle IOException; atomic updates of endOfFile
            val bytesWritten = channel.write(entry)
            if (VERBOSE) log("add - key=$key, value=$value, initialPosition=$initialPosition, bytesWritten=$bytesWritten")

            if (bytesWritten == 0) return@appendToFile
            // channel.force(false)

            endOfFile = initialPosition + bytesWritten
            keyDirectory[key] = initialPosition
            if (VERBOSE) log("add - key=$key, initialPosition=$initialPosition, endOfFile=$endOfFile")
        }
    }

    /**
     * Merges the currently indexed keys that are in memory.
     * Ensure that the index is up-to-date by calling [index]
     */
    fun merge() {
        val file = File(caskPath)
        if (!file.exists()) {
            log("merge - No file")
            return
        }

        val mergedKeys = mutableMapOf<String, Pair<Long, Long>>() // Key, startPosition, endPosition
        index(file) { entry, startPosition, endPosition ->
            mergedKeys[entry.key] = startPosition to endPosition
        }

        val mergeFile = File("$caskPath$MERGE_FILE_EXTENSION")

        val originalOutputStream = FileOutputStream(file, false)
        val mergeOutputStream = FileOutputStream(mergeFile, true)
        originalOutputStream.use { os ->
            val originalChannel = os.channel
            mergeOutputStream.use { ms ->
                val mergeChannel = ms.channel

                for ((key, value) in mergedKeys) {
                    val (startPosition, endPosition) = value

                    val buffer = ByteBuffer.allocate((endPosition - startPosition).toInt())
                    val header = arrayOf(buffer)
                    val readBytes = originalChannel.read(header, 0, 1) // TODO (Gonçalo): Handle IOException

                    if (VERBOSE) log("add - key=$key, startPosition=$startPosition, endPosition=$endPosition, readBytes=$readBytes") // TODO (Gonçalo): Can we force the verbose check to be inlined with an inline method?

                    buffer.flip()
                    mergeChannel.write(buffer)
                }
            }
        }

        -- move new file to old file
    }

    private fun index(file: File, predicate: (entry: Entry, startPosition: Long, endPosition: Long) -> Unit) {
        // TODO (Gonçalo): Save key directory in a file?

        val fis = FileInputStream(file)
        fis.use {
            val channel = it.channel
            var startPosition = channel.position()
            val end = channel.size() // TODO (Gonçalo): Handle IOException
            do {
                val beforePosition = startPosition
                val entry = readEntry(channel)
                val endPosition = channel.position() // TODO (Gonçalo): Handle IOException
                log("index - beforePosition=$beforePosition, endPosition=$endPosition, entry=$entry")
                startPosition = endPosition

                // Note: The key entries should be in order.
                // @fragile: There are several methods that throw, so there's the chance that keyDirectory is left pointing to an older value if something throws and we manage to continue.
                log("index - key=${entry.key}, value=${entry.value}, at=$beforePosition")

                predicate(entry, startPosition, endPosition)

            } while (startPosition < end)
        }
    }

    private fun readEntry(channel: FileChannel): Entry {
        val headerBuffer = ByteBuffer.allocate(HEADER_SIZE)
        val header = arrayOf(headerBuffer)
        val readHeaderBytes = channel.read(header, 0, 1)
        if (VERBOSE) log("read - readHeaderBytes=$readHeaderBytes, headerBuffer=$headerBuffer")

        // TODO (Gonçalo): Check if header was completely read

        headerBuffer.flip()
        val epoch = headerBuffer.int
        val keySize = headerBuffer.int
        val valueSize = headerBuffer.int
        val keyType = headerBuffer.short
        val valueType = headerBuffer.short

        if (VERBOSE) log("read - epoch=$epoch, keySize=$keySize, valueSize=$valueSize, keyType=$keyType, valueType=$valueType")

        val bodyBuffer = ByteBuffer.allocate(keySize + valueSize)
        val body = arrayOf(bodyBuffer)
        val readBodyBytes = channel.read(body, 0, 1)
        if (VERBOSE) log("read - readBodyBytes=$readBodyBytes, bodyBuffer=$bodyBuffer")

        // TODO (Gonçalo): Check if body was completely read

        bodyBuffer.flip()
        val bodyByteArray = bodyBuffer.array()
        val readKey = String(bodyByteArray, 0, keySize)
        val readValue = String(bodyByteArray, keySize, valueSize)
        if (VERBOSE) log("read - readKey=$readKey, readValue=$readValue")

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
        file: File,
        predicate: (fileOutputStream: FileOutputStream) -> Unit
    ) {
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

        if (VERBOSE) log("createEntry - epoch=$epoch, keySize=$keySize, valueSize=$valueSize, keyType=$keyType, valueType=$valueType, key=$key, value=$value")
        return entryNoCrc.flip()
    }

    private fun log(text: String) {
        if (!VERBOSE) return
        println(text) // TODO (Gonçalo): Inject real logger
    }
}