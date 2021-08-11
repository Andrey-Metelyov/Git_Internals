package gitinternals

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.Inflater
import kotlin.io.path.invariantSeparatorsPathString

fun main() {
    println("Enter .git directory location:")
    val gitDir = readLine()!!
    println("Enter command:")
    when (readLine()!!) {
        "list-branches" -> showGitBranches(gitDir)
        "cat-file" -> showGitFile(gitDir)
        "log" -> showGitLog(gitDir)
        "commit-tree" -> showCommitTree(gitDir)
        else -> println("Unknown command")
    }
}

fun showCommitTree(gitDir: String) {
    println("Enter commit-hash")
    val hash = readLine()!!
    val gitCommit = GitCommit.parseFile(gitDir, hash)
    System.err.println(gitCommit.tree)
    getCommitTree(gitDir, "", gitCommit.tree)
}

fun getCommitTree(gitDir: String, root: String, hash: String) {
    val gitTree = GitTree.parseFile(gitDir, hash)
    System.err.println(gitTree)
    for (record in gitTree.listGitTreeRecord) {
        val hash = record.hash
        val path = Path.of(gitDir, "objects", hash.substring(0, 2), hash.substring(2))

        val f = GitFile(path.toFile())
        System.err.println("file type of ${hash}: ${f.type}")
        if (f.type == "tree") {
            getCommitTree(gitDir, record.filename, hash)
        } else {
            println(Path.of(root, record.filename).invariantSeparatorsPathString)
        }
    }
}

fun showGitLog(gitDir: String) {
    println("Enter branch name:")
    val branchName = readLine()!!
    val headsDir = Path.of(gitDir, "refs", "heads")
    val branchFile = File(headsDir.toFile(), branchName)
    var commit = branchFile.readLines()[0]
    while (commit.isNotEmpty()) {
        System.err.println("branchName: $branchName, branchFile: $branchFile, commit: $commit")
        val gitCommit = GitCommit.parseFile(gitDir, commit)
        System.err.println(gitCommit)
        println(
            "Commit: $commit\n" +
                    "${gitCommit.committerName} ${gitCommit.committerEmail} commit timestamp: ${gitCommit.timeCommitted}\n" +
                    "${gitCommit.commitMessage}\n"
        )
        commit = if (gitCommit.parents.isNotEmpty()) gitCommit.parents[0] else ""
        System.err.println("parent: ${commit}")
    }
}

fun showGitBranches(gitDir: String) {
    val (_, currentHeadFileName) = File(gitDir, "HEAD").readLines()[0].split(": ")
    val currentHead = File(gitDir, currentHeadFileName)
    System.err.println("currentHead = '$currentHeadFileName', file: $currentHead")
    val headsDir = Path.of(gitDir, "refs", "heads")
    val branches = headsDir.toFile().listFiles()
    branches.sorted().forEach { println(if (it == currentHead) "* " + it.name else "  " + it.name) }
}

private fun showGitFile(gitDir: String) {
    println("Enter git object hash:")
    val hash = readLine()!!
    printFile(gitDir, hash)
}

private fun printFile(gitDir: String, hash: String) {
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
    System.err.println(headerString)
    val (type, size) = headerString.split(" ")
    System.err.println("type:$type length:$size")
    when (type) {
        "commit" -> {
            printCommitInfo(content)
//            val commit = GitCommit.parse(content.toByteArray())
//            System.err.println(commit)
        }
        "blob" -> printBlobInfo(content)
        "tree" -> printTree(content)
    }
}

fun printTree(content: List<Byte>) {
    println("*TREE*")
    System.err.println(content.map { it.toInt().toChar() })
    var lastIndex = 0
    while (lastIndex < content.size) {
        val zeroByteIndex = content.slice(lastIndex until content.lastIndex).indexOf(0)
        System.err.println("zeroByteIndex: $zeroByteIndex")
        val (perm, filename) = content.slice(lastIndex until lastIndex + zeroByteIndex).map { it.toInt().toChar() }.joinToString("")
            .split(" ")
        lastIndex += zeroByteIndex + 1
        val hashBytes = content.slice(lastIndex until lastIndex + 20)
        System.err.println("hashBytes = $hashBytes")
        System.err.println("hashBytes = ${hashBytes.map { byte -> "%x".format(byte) }}")
        val hash = hashBytes.joinToString("") { byte -> "%x".format(byte) }
        System.err.println("hash.length = ${hash.length}")
        println("$perm $hash $filename")
        lastIndex += 20
        System.err.println("lastIndex: $lastIndex, content.size: ${content.size}")
    }
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
    println("author: $authorName ${authorEmail.substring(1 until authorEmail.lastIndex)} original timestamp: $timeCreated")
    println("committer: $committerName ${committerEmail.substring(1 until committerEmail.lastIndex)} commit timestamp: $timeCommitted")
    println("commit message:$commitMessage")
}
