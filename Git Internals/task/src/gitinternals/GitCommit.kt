package gitinternals

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GitCommit private constructor(
    val tree: String,
    val parents: String,
    val authorName: String,
    val authorEmail: String,
    val timeCreated: String,
    val committerName: String,
    val committerEmail: String,
    val timeCommitted: String,
    val commitMessage: String
) {
    companion object {
        fun parseFile(file: File): GitCommit {
            val gitFile = GitFile(file)
            return parse(gitFile.content)
        }

        private fun parse(bytes: ByteArray): GitCommit {
            val lines = bytes.map { it.toInt().toChar() }.joinToString("").split('\n')
//            println("*COMMIT*")
//            for (line in lines) {
//                System.err.println(line)
//            }
            val (_, tree) = lines[0].split(" ")
            val (_, parents) = lines[1].split(" ")
            val (_, authorName, authorEmail, timeCommitCreated, timezoneCommitCreated) = lines[2].split(" ")
            val (_, committerName, committerEmail, timeCommitApplied, timezoneCommitApplied) = lines[3].split(" ")
            val commitMessage = lines.subList(4, lines.lastIndex).joinToString("\n")

//            println("tree: $tree")
//            println("parents: $parents")
//            System.err.println(timeCommitCreated)
            val commitCreated = Instant.ofEpochSecond(timeCommitCreated.toLong())
            val commitApplied = Instant.ofEpochSecond(timeCommitApplied.toLong())
            val formatterCreated =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ZZZZZ").withZone(ZoneId.of(timezoneCommitCreated))
            val formatterApplied =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ZZZZZ").withZone(ZoneId.of(timezoneCommitApplied))
            val timeCreated = formatterCreated.format(commitCreated)
            val timeCommitted = formatterApplied.format(commitApplied)
//            System.err.println(timeCreated)
//            System.err.println(timeCommitted)
//            println("author: $authorName ${authorEmail.substring(1 until authorEmail.lastIndex)} original timestamp: $timeCreated")
//            println("committer: $committerName ${committerEmail.substring(1 until committerEmail.lastIndex)} commit timestamp: $timeCommitted")
//            println("commit message:$commitMessage")
            return GitCommit(tree, parents,
                authorName, authorEmail, timeCreated,
                committerName, committerEmail, timeCommitted,
                commitMessage
            )
        }
    }

    override fun toString(): String {
        return "GitCommit(tree='$tree', parents='$parents', authorName='$authorName', authorEmail='$authorEmail', timeCreated='$timeCreated', committerName='$committerName', committerEmail='$committerEmail', timeCommitted='$timeCommitted', commitMessage='$commitMessage')"
    }
}