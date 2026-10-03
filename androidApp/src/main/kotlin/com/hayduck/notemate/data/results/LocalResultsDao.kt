package com.hayduck.notemate.data.results

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LocalResultsDao {
    @Transaction
    @Query("SELECT * FROM results_scope")
    fun observeSnapshots(): Flow<List<ResultsSnapshot>>

    @Transaction
    @Query("SELECT * FROM results_scope")
    suspend fun readSnapshots(): List<ResultsSnapshot>

    @Insert
    suspend fun insertScope(scope: ResultsScopeEntity)

    @Insert
    suspend fun insertProposal(proposal: StoredProposalEntity)

    @Update
    suspend fun updateProposal(proposal: StoredProposalEntity): Int

    @Insert
    suspend fun insertActivity(activity: StoredActivityEntity)

    @Query("DELETE FROM activity")
    suspend fun clearActivity()

    @Query("DELETE FROM proposals WHERE id = :proposalId AND revision = :expectedRevision")
    suspend fun deleteProposal(proposalId: String, expectedRevision: Long): Int
}
