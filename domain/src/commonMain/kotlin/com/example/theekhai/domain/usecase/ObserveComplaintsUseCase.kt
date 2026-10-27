package com.example.theekhai.domain.usecase

import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.repository.ComplaintRepository
import kotlinx.coroutines.flow.Flow

class ObserveComplaintsUseCase(
    private val complaintRepository: ComplaintRepository
) {
    operator fun invoke(): Flow<List<Complaint>> = complaintRepository.observeComplaints()
}
