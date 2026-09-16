package dev.aten.rssh.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hosts")
data class Host(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hostname: String,
    val port: Int = 22,
    val username: String,
    /** Base64 SSH public-key blob saved when the user confirms the fingerprint (trust on first use). */
    val knownHostKey: String? = null,
)
