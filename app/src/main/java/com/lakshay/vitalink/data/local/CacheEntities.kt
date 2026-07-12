package com.lakshay.vitalink.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lakshay.vitalink.data.Encounter
import com.lakshay.vitalink.data.LatestVital
import com.lakshay.vitalink.data.Patient

@Entity(tableName = "encounters")
data class EncounterEntity(
    @PrimaryKey val id: Long,
    val status: String?,
    val wardLabel: String?,
    val bedLabel: String?,
    val admittedAt: String?,
    val patientId: Long?,
    val mrn: String?,
    val firstName: String?,
    val lastName: String?,
)

@Entity(tableName = "vitals", primaryKeys = ["encounterId", "type"])
data class VitalEntity(
    val encounterId: Long,
    val type: String,
    val value: Double?,
    val unit: String?,
    val measuredAt: String?,
)

fun Encounter.toEntity() = EncounterEntity(
    id = id,
    status = status,
    wardLabel = wardLabel,
    bedLabel = bedLabel,
    admittedAt = admittedAt,
    patientId = patient?.id,
    mrn = patient?.mrn,
    firstName = patient?.firstName,
    lastName = patient?.lastName,
)

fun EncounterEntity.toDomain() = Encounter(
    id = id,
    status = status,
    wardLabel = wardLabel,
    bedLabel = bedLabel,
    admittedAt = admittedAt,
    patient = if (patientId != null || mrn != null || firstName != null || lastName != null) {
        Patient(patientId, mrn, firstName, lastName)
    } else {
        null
    },
)

fun LatestVital.toEntity(encounterId: Long) = VitalEntity(encounterId, type, value, unit, measuredAt)

fun VitalEntity.toDomain() = LatestVital(type, value, unit, measuredAt)
