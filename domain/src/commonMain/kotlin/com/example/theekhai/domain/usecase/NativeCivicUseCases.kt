package com.example.theekhai.domain.usecase

import com.example.theekhai.domain.repository.NativeCivicRepository

class SignInUseCase(private val repository: NativeCivicRepository) {
    suspend operator fun invoke(email: String, password: String) = repository.signIn(email.trim(), password)
}
class RegisterCitizenUseCase(private val repository: NativeCivicRepository) {
    suspend operator fun invoke(email: String, password: String, fullName: String, ward: String) =
        repository.register(email.trim(), password, fullName.trim(), ward.trim())
}
class SubmitCivicReportUseCase(private val repository: NativeCivicRepository) {
    suspend operator fun invoke(title: String, description: String, category: String, address: String, latitude: Double?, longitude: Double?, consent: Boolean) =
        repository.submitReport(title.trim(), description.trim(), category, address.trim(), latitude, longitude, consent)
}
