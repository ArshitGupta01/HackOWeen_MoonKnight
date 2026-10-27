package com.example.theekhai.domain.usecase

import com.example.theekhai.domain.repository.ComplaintRepository

class SyncOfflineComplaintsUseCase(
    private val complaintRepository: ComplaintRepository
) {
    suspend operator fun invoke(): Result<Int> = complaintRepository.syncOfflineQueue()
}
