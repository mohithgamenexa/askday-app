package app.askday.data

enum class BadgeIcon { spark, sun, moon, flame, crown, brain }

class Badge(val id: String, val title: String, val description: String, val icon: BadgeIcon, val tint: Long)

val BADGES = listOf(
    Badge("first_answer", "First answer", "You answered your first question.", BadgeIcon.spark, 0xFF0A8A80),
    Badge("early_bird", "Early bird", "First in your group to answer.", BadgeIcon.sun, 0xFFF59E0B),
    Badge("night_owl", "Night owl", "Answered after 11 PM.", BadgeIcon.moon, 0xFF4F46E5),
    Badge("streak_7", "One week strong", "A 7-day group streak.", BadgeIcon.flame, 0xFFFF8A3D),
    Badge("streak_30", "Thirty days", "A 30-day group streak.", BadgeIcon.flame, 0xFFF43F5E),
    Badge("streak_100", "Century club", "A 100-day group streak.", BadgeIcon.flame, 0xFFD946EF),
    Badge("crowd_favorite", "Crowd favorite", "Three reactions on one answer.", BadgeIcon.crown, 0xFFEAB308),
    Badge("mind_reader", "Mind reader", "A perfect score in Who said it?", BadgeIcon.brain, 0xFF10B981),
    Badge("question_master", "Question master", "10 of your Asks answered by the whole group.", BadgeIcon.crown, 0xFFFF7A59),
)

fun badgeById(id: String?) = BADGES.firstOrNull { it.id == id }

val STREAK_MILESTONES = mapOf(7 to "streak_7", 30 to "streak_30", 100 to "streak_100")

/** Every 7 days of streak earns a freeze, up to this many saved. */
const val FREEZE_EVERY = 7
const val MAX_FREEZES = 2
