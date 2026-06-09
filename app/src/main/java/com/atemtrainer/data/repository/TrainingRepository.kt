package com.atemtrainer.data.repository

import com.atemtrainer.data.database.AppDatabase
import com.atemtrainer.data.database.SessionEntity
import com.atemtrainer.data.datastore.UserPreferences
import kotlinx.coroutines.flow.Flow

class TrainingRepository(db: AppDatabase, val prefs: UserPreferences) {
    private val dao = db.sessionDao()

    val allSessions: Flow<List<SessionEntity>> = dao.getAllSessions()
    val maxDuration: Flow<Int?> = dao.getMaxDuration()

    suspend fun insertSession(session: SessionEntity) = dao.insertSession(session)
    suspend fun deleteSession(id: Long) = dao.deleteSession(id)
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun getLatestSession() = dao.getLatestSession()
}
