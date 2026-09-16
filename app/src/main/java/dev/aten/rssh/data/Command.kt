package dev.aten.rssh.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "commands",
    foreignKeys = [ForeignKey(Host::class, ["id"], ["hostId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("hostId")],
)
data class Command(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hostId: Long,
    val label: String,
    val command: String,
    val timeoutSec: Int = 30,
)

data class CommandWithHost(
    @Embedded val command: Command,
    @Relation(parentColumn = "hostId", entityColumn = "id") val host: Host,
)
