/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ConsoleKt")

package kotlin.io

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public actual inline fun print(message: Any?) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Int) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Long) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Byte) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Short) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Char) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Boolean) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Float) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Double) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: CharArray) {
    System.out.print(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public actual inline fun println(message: Any?) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Int) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Long) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Byte) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Short) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Char) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Boolean) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Float) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Double) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: CharArray) {
    System.out.println(message)
}

/** Prints the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public actual inline fun println() {
    System.out.println()
}

/**
 * Reads a line of input from the standard input stream.
 *
 * @return the line read or `null` if the input stream is redirected to a file and the end of file has been reached.
 */
fun readLine(): String? = LineReader.readLine(System.`in`, null)

// Singleton object lazy initializes on the first use, internal for tests
internal object LineReader {
    private const val BUFFER_SIZE: Int = 32
    private const val EOL_SIZE: Int = 2 // CRLF (two chars) at most
    private lateinit var decoder: CharsetDecoder
    private var directEOL = false
    private val byteBuffer: ByteBuffer = ByteBuffer.allocate(BUFFER_SIZE)
    private val charBuffer: CharBuffer = CharBuffer.allocate(BUFFER_SIZE)
    private val stringBuilder = StringBuilder()

    @Synchronized
    fun readLine(inputStream: InputStream, charset0: Charset?): String? { // charset == null -> use default
        var read = inputStream.read()
        if (read == -1) return null
        val charset = charset0 ?: Charset.defaultCharset() // use the specified or default charset
        if (!::decoder.isInitialized || decoder.charset() != charset) updateCharset(charset)
        byteBuffer.clear()
        charBuffer.clear()
        stringBuilder.clear()
        do {
            byteBuffer.put(read.toByte())
            // With "directEOL" encoding bytes are batched before being decoded all at once
            if (!directEOL || !byteBuffer.hasRemaining() || read == '\n'.toInt()) {
                tryDecode(false)
                if (endsWithEOL()) break
            }
            read = inputStream.read()
        } while (read != -1)
        tryDecode(true) // throws exception if not decoded bytes are left
        decoder.reset()
        with(charBuffer) {
            var length = position()
            if (length > 0 && get(length - 1) == '\n') {
                length--
                if (length > 0 && get(length - 1) == '\r') {
                    length--
                }
            }
            position(0)
            limit(length)
            stringBuilder.append(charBuffer)
        }
        val result = stringBuilder.toString()
        if (stringBuilder.length > BUFFER_SIZE) trimStringBuilder()
        return result
    }

    private fun updateCharset(charset: Charset) {
        val decoder: CharsetDecoder = charset.newDecoder()
        require(decoder.maxCharsPerByte() <= 1) { "Encodings with multiple chars per byte are not supported" }
        // Only assign decoder if the above check passes, otherwise throw on every readLine call
        this.decoder = decoder
        // try decoding ASCII line separator to see if this charset (like UTF-8) encodes it directly
        byteBuffer.clear()
        charBuffer.clear()
        byteBuffer.put('\n'.toByte())
        byteBuffer.flip()
        decoder.decode(byteBuffer, charBuffer, false)
        directEOL = charBuffer.position() == 1 && charBuffer.get(0) == '\n'
    }

    private fun tryDecode(endOfInput: Boolean) {
        while (true) {
            byteBuffer.flip()
            with(decoder.decode(byteBuffer, charBuffer, endOfInput)) {
                if (isError) throwException()
            }
            byteBuffer.compact()
            if (charBuffer.remaining() >= EOL_SIZE) break
            // offload everything from charBuffer but last EOL_SIZE chars into stringBuilder
            val chars = charBuffer.position()
            charBuffer.position(0)
            charBuffer.limit(chars - EOL_SIZE)
            stringBuilder.append(charBuffer)
            charBuffer.position(chars - EOL_SIZE)
            charBuffer.limit(chars)
            charBuffer.compact()
        }
    }

    private fun endsWithEOL(): Boolean = with(charBuffer) {
        val p = position()
        return p > 0 && get(p - 1) == '\n'
    }

    private fun trimStringBuilder() {
        stringBuilder.setLength(BUFFER_SIZE)
        stringBuilder.trimToSize()
    }
}
