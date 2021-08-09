package gitinternals

import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.util.zip.Inflater

fun main() {
    // write your code here
    println("Enter .git directory location:")
    val gitDir = readLine()!!
    println("Enter git object hash:")
    val hash = readLine()!!
    val path = Path.of(gitDir, "objects", hash.substring(0, 2), hash.substring(2))
    val inflater = Inflater()
    val outputStream = ByteArrayOutputStream()
    val buffer = ByteArray(1024)

    inflater.setInput(path.toFile().readBytes())

    while (!inflater.finished()) {
        val count = inflater.inflate(buffer)
        outputStream.write(buffer, 0, count)
    }

    outputStream.close()
    val bytes = outputStream.toByteArray()
    val zeroByteIndex = bytes.indexOf(0)
    System.err.println("zeroByteIndex=$zeroByteIndex")
    val header = bytes.slice(0 until zeroByteIndex).map { it.toInt().toChar() }
    val headerString = header.joinToString("")
//    val (header, content) = outputStream.toString().split(0.toChar())
    System.err.println(headerString)
    val (type, size) = headerString.split(" ")
    println("type:$type length:$size")
//    println(content)
//    println(outputStream.toString().replace(0.toChar(), '\n'))
}
