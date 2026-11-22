package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
  @PrimaryKey val packageNameHash: String,
  val encryptedPayloadB64: String,
  val ivB64: String,
  val addedAtEpochMs: Long,
)

@Entity(tableName = "calc_history")
data class CalcHistoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val expression: String,
  val result: String,
  val timestampEpochMs: Long,
)
