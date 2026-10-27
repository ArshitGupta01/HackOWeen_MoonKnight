package com.example.theekhai.domain.model

data class SignedInUser(val id: String, val email: String, val fullName: String, val role: String)
data class SubmittedReport(val id: String, val title: String, val status: String)
