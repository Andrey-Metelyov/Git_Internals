package gitinternals

import java.io.File
import java.nio.file.Path

class GitTree private constructor(
    val listGitTreeRecord: List<GitTreeRecord>)
{
    class GitTreeRecord(val permissions: String, val hash: String, val filename: String)
    companion object {
        fun parseFile(gitDir: String, hash: String): GitTree {
            val path = Path.of(gitDir, "objects", hash.substring(0, 2), hash.substring(2))
            return parseFile(path.toFile())
        }
        fun parseFile(file: File): GitTree {
            val gitFile = GitFile(file)
            return parse(gitFile.content)
        }

        private fun parse(content: ByteArray): GitTree {
            var lastIndex = 0
            val listGitTreeRecord = mutableListOf<GitTreeRecord>()
            while (lastIndex < content.size) {
                val zeroByteIndex = content.slice(lastIndex until content.lastIndex).indexOf(0)
//                System.err.println("zeroByteIndex: $zeroByteIndex")
                val (perm, filename) = content.slice(lastIndex until lastIndex + zeroByteIndex).map { it.toInt().toChar() }.joinToString("")
                    .split(" ")
                lastIndex += zeroByteIndex + 1
                val hashBytes = content.slice(lastIndex until lastIndex + 20)
//                System.err.println("hashBytes = $hashBytes")
//                System.err.println("hashBytes = ${hashBytes.map { byte -> "%x".format(byte) }}")
                val hash = hashBytes.joinToString("") { byte -> "%02x".format(byte) }
//                System.err.println("hash.length = ${hash.length}")
//                System.err.println("$perm $hash $filename")
                listGitTreeRecord.add(GitTreeRecord(perm, hash, filename))
                lastIndex += 20
//                System.err.println("lastIndex: $lastIndex, content.size: ${content.size}")
            }
            return GitTree(listGitTreeRecord)
        }
    }

    override fun toString(): String {
        return "GitTree(listGitTreeRecord=\n${
            listGitTreeRecord.map {
                "\t${it.permissions} ${it.filename} ${it.hash}\n" }})"
    }
}