package com.yash.tracker.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class HabitRecord(
    val id: String = UUID.randomUUID().toString(),
    // Automatic Fields (Captured at Creation)
    val session_date: String, // YYYY-MM-DD
    val session_time: String, // HH:mm:ss

    // Part 1: Core 20 Questions
    // A. Session Context
    val location: String,
    val activity_before: String,
    val alone: String? = null,

    // B. Triggers and Influences
    val triggers: List<String>,
    val primary_trigger: String,
    val urge_intensity: Int, // 1-10
    val urge_duration: String? = null,

    // C. Emotional State
    val emotions_before: List<String>,
    val motivation: String? = null,
    val emotions_after: List<String>,

    // D. Control and Media
    val planned: String,
    val tried_resisting: String,
    val media_consumed: String,
    val stimulation_type: List<String>? = null,
    val unexpected_attention: String? = null,

    // E. Outcome and Reflection
    val experience_satisfaction: Int, // 1-10
    val decision_satisfaction: String,
    val activity_impact: String,
    val perceived_control: String,
    val sleep_duration: String? = null,

    // Part 4: Conditional Questions
    val online_platform: String? = null,
    val stress_source: String? = null,
    val spontaneous_reason: String? = null,
    val intentional_media: String? = null,
    val affected_activity: String? = null
)
