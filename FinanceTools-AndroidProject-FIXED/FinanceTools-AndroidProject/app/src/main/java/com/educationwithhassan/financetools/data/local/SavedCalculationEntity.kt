package com.educationwithhassan.financetools.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_calculations")
data class SavedCalculationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolId: String,
    val toolName: String,
    val categoryId: String,
    val currencyCode: String,
    val inputsJson: String,
    val primaryLabel: String,
    val primaryResult: String,
    val summaryDetails: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_tools")
data class FavoriteToolEntity(
    @PrimaryKey val toolId: String,
    val addedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_tools")
data class RecentToolEntity(
    @PrimaryKey val toolId: String,
    val lastOpenedEpochMs: Long = System.currentTimeMillis()
)
