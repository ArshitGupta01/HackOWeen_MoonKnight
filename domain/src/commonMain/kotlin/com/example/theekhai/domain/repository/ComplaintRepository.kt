package com.example.theekhai.domain.repository

import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.domain.model.ComplaintStatus
import kotlinx.coroutines.flow.Flow

interface ComplaintRepository {
    fun observeComplaints(): Flow<List<Complaint>>
    suspend fun getComplaintById(id: String): Complaint?
    suspend fun createComplaint(complaint: Complaint): Complaint
    suspend fun updateComplaint(complaint: Complaint): Complaint
    suspend fun updateStatus(id: String, status: ComplaintStatus): Boolean
    suspend fun updateTriage(
        id: String,
        clusterId: String?,
        priority: ComplaintPriority,
        category: String
    ): Boolean
    suspend fun resolveComplaint(
        id: String,
        workerId: String,
        resolutionNotes: String,
        photoUrl: String
    ): Boolean
    suspend fun syncOfflineQueue(): Result<Int>
}
