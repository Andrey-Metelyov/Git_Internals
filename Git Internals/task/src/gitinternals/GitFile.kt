package gitinternals

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Inflater

class GitFile(file: File) {
    val type: String
//        get() = field
    val contentSize: Int
//        get() = field
    val content: ByteArray
//        public get() = field

    init {
        val inflater = Inflater()
        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)

        inflater.setInput(file.readBytes())

        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        outputStream.close()

        val bytes = outputStream.toByteArray()
        val zeroByteIndex = bytes.indexOf(0)
//        System.err.println("zeroByteIndex=$zeroByteIndex")
        val header = bytes.slice(0 until zeroByteIndex).map { it.toInt().toChar() }
        this.content = bytes.sliceArray(zeroByteIndex + 1..bytes.lastIndex)
        val headerString = header.joinToString("")
//        System.err.println(headerString)
        val (type, size) = headerString.split(" ")
        this.type = type
        this.contentSize = size.toInt()
        System.err.println("type:$type length:$size")
//        System.err.println(this.content.map {it.toInt().toChar()}.joinToString(""))
    }
}
