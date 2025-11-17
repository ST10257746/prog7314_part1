package com.example.prog7314_part1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Goal entity for Room Database
 * Daily goals and to-do items
 */
@Entity(
    tableName = "goals",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId"), Index("date")]
)
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val goalId: Long = 0,
    
    val userId: String,
    val date: String,  // "yyyy-MM-dd" - specific day goal
    
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
