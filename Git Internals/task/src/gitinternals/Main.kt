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
        "tree" -> printTree(content)
    }
//    println(content)
//    println(outputStream.toString().replace(0.toChar(), '\n'))
}

fun printTree(content: List<Byte>) {
//  [type][space][size][nullChar]
//  [firstPermisionNumber][space][firstFileName][nullChar][firstHash]
//  [secondPermissionNumber][space][secondFileName][nullChar][secondHash]
//  [thirdPermissionNumber][space][secondFileName][nullChar][secondHash]
    println("*TREE*")
    System.err.println(content.map { it.toInt().toChar() })
    var lastIndex = 0
    while (lastIndex < content.size) {
        val zeroByteIndex = content.slice(lastIndex until content.lastIndex).indexOf(0)
        System.err.println("zeroByteIndex: $zeroByteIndex")
        val (perm, filename) = content.slice(lastIndex until lastIndex + zeroByteIndex).map { it.toInt().toChar() }.joinToString("")
            .split(" ")
//    System.err.println("filename: $filename")
        lastIndex += zeroByteIndex + 1
        val hashBytes = content.slice(lastIndex until lastIndex + 20)
        System.err.println("hashBytes = ${hashBytes.toString()}")
        System.err.println("hashBytes = ${hashBytes.map { byte -> "%x".format(byte) }}")
        val hash = hashBytes.map { byte -> "%x".format(byte) }.joinToString("")
        System.err.println("hash.length = ${hash.length}")
        println("$perm $hash $filename")
        lastIndex += 20
        System.err.println("lastIndex: $lastIndex, content.size: ${content.size}")
    }
//    val line = content.slice(lastIndex until zeroByteIndex).map { it.toInt().toChar() }.joinToString("")
// Output text at line (3)
// (100644 2b26c15c04375d90203783fb4c2a45ff04b571a6 main.kt) does not match expected
// (100644 2b26c15c04375d90203783fb4c2a45ff04b571a6 main.kt)
// 2b26c15c4375d90203783fb4c2a45ff4b571a6
// [1, 0, 0, 6, 4, 4,  , m, a, i, n, ., k, t,  ,
// +, &, ￁, \, , 7, ], ﾐ,  , 7, ﾃ, ￻, L, *, E, ￿, , ﾵ, q, ﾦ,
// 1, 0, 0, 6, 4, 4,  , r, e, a, d, m, e, ., t, x, t,  ,
// J, ﾊ, ﾾ, {, a, ﾍ, ￟, ﾜ, U, ﾭ, ﾾ, ﾣ, Y, ￎ, ﾉ, , u, y, J, a]
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
