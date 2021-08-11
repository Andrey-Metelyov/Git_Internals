package gitinternals

import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GitCommit private constructor(
    val tree: String,
    val parents: List<String>,
    val authorName: String,
    val authorEmail: String,
    val timeCreated: String,
    val committerName: String,
    val committerEmail: String,
    val timeCommitted: String,
    val commitMessage: String
) {
    companion object {
        fun parseFile(gitDir: String, hash: String): GitCommit {
            val path = Path.of(gitDir, "objects", hash.substring(0, 2), hash.substring(2))
            return parseFile(path.toFile())
        }
        fun parseFile(file: File): GitCommit {
            val gitFile = GitFile(file)
            return parse(gitFile.content)
        }

        private fun parse(bytes: ByteArray): GitCommit {
            val lines = bytes.map { it.toInt().toChar() }.joinToString("").split('\n')
            System.err.println("*COMMIT*")
            for (line in lines) {
                System.err.println(line)
            }
            var current = 0
            val (_, tree) = lines[current++].split(" ")
            val listParents = mutableListOf<String>()
            do {
                val (type, str) = lines[current++].split(" ")
                if (type == "parent") {
                    listParents.add(str)
                } else {
                    current--
                }
            } while (type == "parent")
            val (_, authorName, authorEmail, timeCommitCreated, timezoneCommitCreated) = lines[current++].split(" ")
            val (_, committerName, committerEmail, timeCommitApplied, timezoneCommitApplied) = lines[current++].split(" ")
            current++
            val commitMessage = lines.subList(current++, lines.lastIndex).joinToString("\n")

            val commitCreated = Instant.ofEpochSecond(timeCommitCreated.toLong())
            val commitApplied = Instant.ofEpochSecond(timeCommitApplied.toLong())
            val formatterCreated =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ZZZZZ").withZone(ZoneId.of(timezoneCommitCreated))
            val formatterApplied =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ZZZZZ").withZone(ZoneId.of(timezoneCommitApplied))
            val timeCreated = formatterCreated.format(commitCreated)
            val timeCommitted = formatterApplied.format(commitApplied)

            return GitCommit(tree, listParents,
                authorName, authorEmail.substring(1 until authorEmail.lastIndex), timeCreated,
                committerName, committerEmail.substring(1 until committerEmail.lastIndex), timeCommitted,
                commitMessage
            )
        }
    }

    override fun toString(): String {
        return "GitCommit(tree='$tree',\nparents='$parents',\nauthorName='$authorName', authorEmail='$authorEmail', timeCreated='$timeCreated',\ncommitterName='$committerName', committerEmail='$committerEmail', timeCommitted='$timeCommitted',\ncommitMessage='$commitMessage')"
    }
}