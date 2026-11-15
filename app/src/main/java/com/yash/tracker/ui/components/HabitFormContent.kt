package com.yash.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yash.tracker.data.model.HabitRecord
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HabitFormContent(
    initialRecord: HabitRecord? = null,
    onSubmit: (HabitRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var location by remember { mutableStateOf(initialRecord?.location ?: "") }
    var activityBefore by remember { mutableStateOf(initialRecord?.activity_before ?: "") }
    var alone by remember { mutableStateOf(initialRecord?.alone) }

    var triggers by remember { mutableStateOf(initialRecord?.triggers ?: emptyList<String>()) }
    var primaryTrigger by remember { mutableStateOf(initialRecord?.primary_trigger ?: "") }
    var urgeIntensity by remember { mutableFloatStateOf(initialRecord?.urge_intensity?.toFloat() ?: 5f) }
    var urgeDuration by remember { mutableStateOf(initialRecord?.urge_duration) }

    var emotionsBefore by remember { mutableStateOf(initialRecord?.emotions_before ?: emptyList<String>()) }
    var motivation by remember { mutableStateOf(initialRecord?.motivation) }
    var emotionsAfter by remember { mutableStateOf(initialRecord?.emotions_after ?: emptyList<String>()) }

    var planned by remember { mutableStateOf(initialRecord?.planned ?: "") }
    var triedResisting by remember { mutableStateOf(initialRecord?.tried_resisting ?: "") }
    var mediaConsumed by remember { mutableStateOf(initialRecord?.media_consumed ?: "") }
    var stimulationType by remember { mutableStateOf(initialRecord?.stimulation_type) }
    var unexpectedAttention by remember { mutableStateOf(initialRecord?.unexpected_attention) }

    var experienceSatisfaction by remember { mutableFloatStateOf(initialRecord?.experience_satisfaction?.toFloat() ?: 5f) }
    var decisionSatisfaction by remember { mutableStateOf(initialRecord?.decision_satisfaction ?: "") }
    var activityImpact by remember { mutableStateOf(initialRecord?.activity_impact ?: "") }
    var perceivedControl by remember { mutableStateOf(initialRecord?.perceived_control ?: "") }
    var sleepDuration by remember { mutableStateOf(initialRecord?.sleep_duration) }

    // Conditional Fields
    var onlinePlatform by remember { mutableStateOf(initialRecord?.online_platform) }
    var stressSource by remember { mutableStateOf(initialRecord?.stress_source) }
    var spontaneousReason by remember { mutableStateOf(initialRecord?.spontaneous_reason) }
    var intentionalMedia by remember { mutableStateOf(initialRecord?.intentional_media) }
    var affectedActivity by remember { mutableStateOf(initialRecord?.affected_activity) }

    // Cleanup logic for conditional fields
    LaunchedEffect(triggers) {
        if ("Something online" !in triggers) onlinePlatform = null
        if ("Stress" !in triggers) stressSource = null
        if (primaryTrigger !in triggers) primaryTrigger = triggers.firstOrNull() ?: ""
    }
    LaunchedEffect(planned) {
        if (planned != "Spontaneous") spontaneousReason = null
    }
    LaunchedEffect(mediaConsumed) {
        if (mediaConsumed != "Yes") intentionalMedia = null
    }
    LaunchedEffect(activityImpact) {
        if (activityImpact == "No") affectedActivity = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (initialRecord != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Session Date: ${initialRecord.session_date}", style = MaterialTheme.typography.bodySmall)
                    Text("Session Time: ${initialRecord.session_time}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Section A: Context
        SectionTitle("Session Context")
        SingleSelectGroup("Where were you?", listOf("Bedroom", "Bathroom", "Living room", "Workplace", "Outdoors", "Other"), location) { location = it }
        SingleSelectGroup("What were you doing beforehand?", listOf("Scrolling social media", "Watching videos", "Working / Studying", "Gaming", "Lying in bed", "Just woke up", "Trying to sleep", "Nothing particular", "Other"), activityBefore) { activityBefore = it }
        SingleSelectGroup("Were you alone?", listOf("Yes", "No"), alone) { alone = it }

        // Section B: Triggers
        SectionTitle("Triggers and Influences")
        MultiSelectGroup("What influenced this moment?", listOf("Something online", "Something I saw", "Something on my mind", "Boredom", "Stress", "Anxiety", "Loneliness", "Physical feelings", "Daily routine", "Difficulty sleeping", "Something unexpected", "Other"), triggers) { triggers = it }
        
        if ("Something online" in triggers) {
            SingleSelectGroup("Which platform?", listOf("Instagram", "TikTok", "YouTube", "Reddit", "Website", "Other"), onlinePlatform) { onlinePlatform = it }
        }
        if ("Stress" in triggers) {
            SingleSelectGroup("Source of stress?", listOf("Work / Studies", "Relationships", "Personal matters", "Financial matters", "General stress", "Other"), stressSource) { stressSource = it }
        }

        if (triggers.isNotEmpty()) {
            SingleSelectGroup("Primary influence?", triggers, primaryTrigger) { primaryTrigger = it }
        }

        SliderWithLabel("How strong was the feeling? (1-10)", urgeIntensity) { urgeIntensity = it }
        SingleSelectGroup("Urge duration?", listOf("Less than 5 minutes", "5–15 minutes", "15–30 minutes", "30–60 minutes", "More than an hour"), urgeDuration) { urgeDuration = it }

        // Section C: Emotional State
        SectionTitle("Emotional State")
        MultiSelectGroup("Feeling beforehand?", listOf("Happy", "Relaxed", "Bored", "Lonely", "Stressed", "Anxious", "Sad", "Angry", "Frustrated", "Tired", "Excited", "Neutral", "Other"), emotionsBefore) { emotionsBefore = it }
        SingleSelectGroup("What were you looking for?", listOf("Entertainment", "Relaxation", "Emotional comfort", "Stress relief", "A distraction", "Better sleep", "Personal enjoyment", "Nothing specific", "Other"), motivation) { motivation = it }
        MultiSelectGroup("Feeling afterward?", listOf("Happy", "Relaxed", "Satisfied", "Sleepy", "Neutral", "Guilty", "Regretful", "Sad", "Frustrated", "Disappointed", "Emotionally drained", "Other"), emotionsAfter) { emotionsAfter = it }

        // Section D: Control and Media
        SectionTitle("Control and Behavior")
        SingleSelectGroup("Planned or spontaneous?", listOf("Planned", "Spontaneous"), planned) { planned = it }
        if (planned == "Spontaneous") {
            SingleSelectGroup("Why spontaneous?", listOf("A sudden feeling", "Something I saw", "Boredom", "Stress", "Habit", "Other"), spontaneousReason) { spontaneousReason = it }
        }
        SingleSelectGroup("Did you try to postpone it?", listOf("Yes", "No", "Not sure"), triedResisting) { triedResisting = it }
        SingleSelectGroup("Media consumed beforehand?", listOf("Yes", "No", "Not sure"), mediaConsumed) { mediaConsumed = it }
        if (mediaConsumed == "Yes") {
            SingleSelectGroup("Intentional search?", listOf("Yes", "No", "Partially"), intentionalMedia) { intentionalMedia = it }
        }
        MultiSelectGroup("Stimulation type?", listOf("Social media", "Videos", "Something I read", "Imagination", "Memories", "Physical sensations", "Other", "None"), stimulationType ?: emptyList()) { stimulationType = it }

        // Section E: Outcome
        SectionTitle("Outcome and Reflection")
        SliderWithLabel("Experience satisfaction? (1-10)", experienceSatisfaction) { experienceSatisfaction = it }
        SingleSelectGroup("Satisfied with decision?", listOf("Yes", "Somewhat", "No", "Unsure"), decisionSatisfaction) { decisionSatisfaction = it }
        SingleSelectGroup("Activity impact?", listOf("No", "Delayed something", "Interrupted something", "Missed something", "Other"), activityImpact) { activityImpact = it }
        if (activityImpact != "No" && activityImpact.isNotEmpty()) {
            SingleSelectGroup("Affected activity?", listOf("Work", "Studies", "Sleep", "Exercise", "Social plans", "Personal responsibilities", "Other"), affectedActivity) { affectedActivity = it }
        }
        SingleSelectGroup("Perceived control?", listOf("Complete control", "Mostly in control", "Partially in control", "Very little control"), perceivedControl) { perceivedControl = it }
        SingleSelectGroup("Sleep last night?", listOf("Less than 4 hours", "4–6 hours", "6–8 hours", "8–10 hours", "More than 10 hours", "Not sure"), sleepDuration) { sleepDuration = it }

        Button(
            onClick = {
                val now = LocalDate.now().toString()
                val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                
                val record = HabitRecord(
                    id = initialRecord?.id ?: java.util.UUID.randomUUID().toString(),
                    session_date = initialRecord?.session_date ?: now,
                    session_time = initialRecord?.session_time ?: time,
                    location = location,
                    activity_before = activityBefore,
                    alone = alone,
                    triggers = triggers,
                    primary_trigger = primaryTrigger,
                    urge_intensity = urgeIntensity.toInt(),
                    urge_duration = urgeDuration,
                    emotions_before = emotionsBefore,
                    motivation = motivation,
                    emotions_after = emotionsAfter,
                    planned = planned,
                    tried_resisting = triedResisting,
                    media_consumed = mediaConsumed,
                    stimulation_type = stimulationType,
                    unexpected_attention = unexpectedAttention,
                    experience_satisfaction = experienceSatisfaction.toInt(),
                    decision_satisfaction = decisionSatisfaction,
                    activity_impact = activityImpact,
                    perceived_control = perceivedControl,
                    sleep_duration = sleepDuration,
                    online_platform = onlinePlatform,
                    stress_source = stressSource,
                    spontaneous_reason = spontaneousReason,
                    intentional_media = intentionalMedia,
                    affected_activity = affectedActivity
                )
                onSubmit(record)
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            enabled = location.isNotEmpty() && activityBefore.isNotEmpty() && triggers.isNotEmpty() && primaryTrigger.isNotEmpty()
        ) {
            Text(if (initialRecord == null) "Submit Log" else "Update Record")
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SingleSelectGroup(label: String, options: List<String>, selected: String?, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiSelectGroup(label: String, options: List<String>, selected: List<String>, onSelected: (List<String>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSelected = option in selected
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) onSelected(selected - option)
                        else onSelected(selected + option)
                    },
                    label = { Text(option) }
                )
            }
        }
    }
}

@Composable
fun SliderWithLabel(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text("$label: ${value.toInt()}", style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = onValueChange, valueRange = 1f..10f, steps = 8)
    }
}
