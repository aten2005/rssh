package dev.aten.rssh.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HostDao {
    @Query("SELECT * FROM hosts ORDER BY name")
    fun observeAll(): Flow<List<Host>>

    @Query("SELECT * FROM hosts WHERE id = :id")
    suspend fun get(id: Long): Host?

    @Upsert
    suspend fun upsert(host: Host): Long

    @Delete
    suspend fun delete(host: Host)

    @Query("UPDATE hosts SET knownHostKey = :key WHERE id = :id")
    suspend fun setKnownHostKey(id: Long, key: String?)
}

@Dao
interface CommandDao {
    @Transaction
    @Query("SELECT * FROM commands ORDER BY label")
    fun observeAllWithHost(): Flow<List<CommandWithHost>>

    @Query("SELECT * FROM commands WHERE id = :id")
    suspend fun get(id: Long): Command?

    @Transaction
    @Query("SELECT * FROM commands WHERE id = :id")
    suspend fun getWithHost(id: Long): CommandWithHost?

    @Upsert
    suspend fun upsert(command: Command): Long

    @Delete
    suspend fun delete(command: Command)
}

@Dao
interface TileSlotDao {
    @Query("SELECT * FROM tile_slots")
    fun observeAll(): Flow<List<TileSlot>>

    @Query("SELECT * FROM tile_slots WHERE slotIndex = :slot")
    suspend fun get(slot: Int): TileSlot?

    @Upsert
    suspend fun upsert(slot: TileSlot)

    @Query("DELETE FROM tile_slots WHERE slotIndex = :slot")
    suspend fun clear(slot: Int)
}
