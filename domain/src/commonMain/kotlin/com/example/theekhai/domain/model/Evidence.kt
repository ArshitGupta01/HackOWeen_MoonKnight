package com.example.theekhai.domain.model

data class EvidenceValidation(
    val complaintId: String,
    val isGpsMatched: Boolean,
    val distanceMeters: Double,
    val detectedVisualElements: List<String>,
    val anomalyDetected: Boolean,
    val resolutionConfidence: Double,
    val reasoning: String,
    val approved: Boolean
)
