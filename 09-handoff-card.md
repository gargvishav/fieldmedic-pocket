# Offline Handoff Card

**Status:** TBD — built late week 2.

## Why this matters

Most "AI medic" apps stop at giving advice. The handoff card is what makes FieldMedic look like a **real product**, not a chatbot. When help arrives (paramedic, family, neighbor), the helper hands over the phone and the card tells the story.

This is also what differentiates us from a generic crisis chatbot in the judges' eyes.

## What this file will contain

- Room schema for stored incidents
- UI design: card layout (photo + timeline + actions taken + vitals if entered)
- Auto-population from triage response (`handoff_summary` field)
- Manual fields: vitals, time, location (optional)
- Export options: shareable image, QR code, plaintext for SMS

## Schema (draft)

```kotlin
@Entity
data class Incident(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val photoPath: String?,
    val severity: String,  // RED / YELLOW / GREEN
    val headline: String,
    val helperActions: List<String>,
    val vitalsNotes: String?,
    val location: String?,
    val handoffSummary: String
)
```

## UX requirements

- One-tap "Show paramedic" button on home screen
- Card readable at arm's length in low light
- No login, no account — privacy first
- Auto-expire records after 72 hours unless user pins them

## Pass criteria for "done"

- Triage response auto-creates an incident record
- Card renders correctly with photo, severity color, summary
- Shareable as image + plaintext
- Works in airplane mode
