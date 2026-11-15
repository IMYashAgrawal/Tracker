package com.yash.tracker.analytics.engine

import com.yash.tracker.analytics.model.DateRangeFilter
import com.yash.tracker.analytics.model.EmotionalMetrics
import com.yash.tracker.analytics.utils.DateHelpers
import com.yash.tracker.analytics.utils.Statistics
import com.yash.tracker.data.model.HabitRecord

object EmotionalAnalyzer {

    fun analyze(
        records: List<HabitRecord>,
        filter: DateRangeFilter
    ): EmotionalMetrics {
        val (start, end) = filter.getEffectiveRange()
        val currentRecords = records.filter {
            val date = DateHelpers.parseDate(it.session_date)
            !date.isBefore(start) && !date.isAfter(end)
        }

        val beforeRanking = currentRecords.flatMap { it.emotions_before }
            .groupBy { it }.map { it.key to it.value.size }.sortedByDescending { it.second }

        val afterRanking = currentRecords.flatMap { it.emotions_after }
            .groupBy { it }.map { it.key to it.value.size }.sortedByDescending { it.second }

        val motivationRanking = currentRecords.mapNotNull { it.motivation }
            .groupBy { it }.map { it.key to it.value.size }.sortedByDescending { it.second }

        val urgeByEmotion = currentRecords.flatMap { r -> r.emotions_before.map { it to r.urge_intensity } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.average() }

        val decisionSatisfaction = currentRecords.groupBy { it.decision_satisfaction }
            .mapValues { Statistics.safeDivide(it.value.size * 100, currentRecords.size) }

        return EmotionalMetrics(
            topEmotionsBefore = beforeRanking,
            topEmotionsAfter = afterRanking,
            motivationRanking = motivationRanking,
            avgSatisfactionByTrigger = currentRecords.groupBy { it.primary_trigger }.mapValues { it.value.map { r -> r.experience_satisfaction }.average() },
            avgUrgeByEmotionBefore = urgeByEmotion,
            decisionSatisfactionPercents = decisionSatisfaction,
            recordIds = currentRecords.map { it.id }
        )
    }
}
