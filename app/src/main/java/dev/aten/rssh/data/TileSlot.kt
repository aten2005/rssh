package dev.aten.rssh.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

const val TILE_SLOT_COUNT = 5

@Entity(
    tableName = "tile_slots",
    foreignKeys = [ForeignKey(Command::class, ["id"], ["commandId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("commandId")],
)
data class TileSlot(
    @PrimaryKey val slotIndex: Int,
    val commandId: Long,
)
