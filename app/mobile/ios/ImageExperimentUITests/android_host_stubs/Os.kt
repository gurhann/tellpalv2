package android.system

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

object OsConstants { const val S_IFMT = 0xf000; const val S_IFDIR = 0x4000; const val S_IFREG = 0x8000 }
data class StructStat(val st_mode: Int)
object Os {
    fun lstat(path: String): StructStat {
        val file = Paths.get(path)
        return StructStat(when {
            Files.isSymbolicLink(file) -> 0xa000
            Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS) -> OsConstants.S_IFDIR
            Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) -> OsConstants.S_IFREG
            else -> 0
        })
    }
    fun chmod(path: String, mode: Int) {
        val bits = listOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE,
            PosixFilePermission.OTHERS_EXECUTE)
        Files.setPosixFilePermissions(Paths.get(path), bits.filterIndexed { index, _ -> mode and (1 shl (8 - index)) != 0 }.toSet())
    }
}
