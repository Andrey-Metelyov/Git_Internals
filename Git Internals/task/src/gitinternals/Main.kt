package gitinternals

import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    val content = bytes.slice(zeroByteIndex + 1..bytes.lastIndex)
    val headerString = header.joinToString("")
//    val (header, content) = outputStream.toString().split(0.toChar())
    System.err.println(headerString)
    val (type, size) = headerString.split(" ")
    System.err.println("type:$type length:$size")
    when (type) {
        "commit" -> printCommitInfo(content)
        "blob" -> printBlobInfo(content)
    }
//    println(content)
//    println(outputStream.toString().replace(0.toChar(), '\n'))
}

fun printBlobInfo(content: List<Byte>) {
    val lines = content.map { it.toInt().toChar() }.joinToString("")
    println("*BLOB*")
    println(lines)
}

fun printCommitInfo(content: List<Byte>) {
    val lines = content.map { it.toInt().toChar() }.joinToString("").split('\n')
    println("*COMMIT*")
    for (line in lines) {
        System.err.println(line)
    }
    val (_, tree) = lines[0].split(" ")
    val (_, parents) = lines[1].split(" ")
    val (_, authorName, authorEmail, timeCommitCreated, timezoneCommitCreated) = lines[2].split(" ")
    val (_, committerName, committerEmail, timeCommitApplied, timezoneCommitApplied) = lines[3].split(" ")
    val commitMessage = lines.subList(4, lines.lastIndex).joinToString("\n")

    println("tree: $tree")
    println("parents: $parents")
    System.err.println(timeCommitCreated)
    val commitCreated = Instant.ofEpochSecond(timeCommitCreated.toLong())
    val commitApplied = Instant.ofEpochSecond(timeCommitApplied.toLong())
    val formatterCreated =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ZZZZZ").withZone(ZoneId.of(timezoneCommitCreated))
    val formatterApplied =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ZZZZZ").withZone(ZoneId.of(timezoneCommitApplied))
    val timeCreated = formatterCreated.format(commitCreated)
    val timeCommitted = formatterApplied.format(commitApplied)
    System.err.println(timeCreated)
    System.err.println(timeCommitted)
    println("author: $authorName ${authorEmail.substring(1..authorEmail.lastIndex - 1)} original timestamp: $timeCreated")
    println("committer: $committerName ${committerEmail.substring(1..committerEmail.lastIndex - 1)} commit timestamp: $timeCommitted")
    println("commit message:$commitMessage")
}
