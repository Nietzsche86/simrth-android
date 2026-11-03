package id.mts.rth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OutboxMutationDto(
    val id: String,
    val tableNameTarget: String,
    val priorityOrder: Int,
    val recordId: String,
    val actionType: String,
    val payloadJson: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SyncPushRequest(
    val mutations: List<OutboxMutationDto>
)

@Serializable
data class SyncPushResponse(
    val acceptedMutationIds: List<String>,
    val rejectedMutationIds: List<String> = emptyList(),
    val serverTime: Long
)
