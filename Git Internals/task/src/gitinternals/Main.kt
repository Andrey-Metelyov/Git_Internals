package gitinternals

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Inflater

fun main() {
    // write your code here
    println("Enter git object location:")
    val location = readLine()!!
    val inflater = Inflater()
    val outputStream = ByteArrayOutputStream()
    val buffer = ByteArray(1024)

    inflater.setInput(File(location).readBytes())

    while (!inflater.finished()) {
        val count = inflater.inflate(buffer)
        outputStream.write(buffer, 0, count)
    }

    outputStream.close()

    println(outputStream.toString().replace(0.toChar(), '\n'))
}
