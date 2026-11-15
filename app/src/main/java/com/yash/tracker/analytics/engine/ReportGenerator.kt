package com.yash.tracker.analytics.engine

import com.yash.tracker.analytics.model.DateRangeFilter
import com.yash.tracker.data.model.HabitRecord
import java.lang.StringBuilder

object ReportGenerator {

    fun generateCsv(records: List<HabitRecord>): String {
        val sb = StringBuilder()
        // Header
        sb.append("ID,Date,Time,Location,Primary Trigger,Urge Intensity,Satisfaction,Planned,Control\n")
        
        records.forEach { r ->
            sb.append("${r.id},")
            sb.append("${r.session_date},")
            sb.append("${r.session_time},")
            sb.append("\"${r.location}\",")
            sb.append("\"${r.primary_trigger}\",")
            sb.append("${r.urge_intensity},")
            sb.append("${r.experience_satisfaction},")
            sb.append("${r.planned},")
            sb.append("\"${r.perceived_control}\"\n")
        }
        
        return sb.toString()
    }

    fun generateSummary(records: List<HabitRecord>, filter: DateRangeFilter): String {
        val (start, end) = filter.getEffectiveRange()
        val current = records.filter { /* filter logic */ true } // Simplified for now
        
        return """
            Habit Tracker Summary Report
            Period: $start to $end
            Total Sessions: ${current.size}
            Avg Urge Intensity: ${String.format("%.1f", current.map { it.urge_intensity }.average())}
            Avg Satisfaction: ${String.format("%.1f", current.map { it.experience_satisfaction }.average())}
        """.trimIndent()
    }
}
