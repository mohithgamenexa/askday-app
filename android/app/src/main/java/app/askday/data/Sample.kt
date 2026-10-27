package app.askday.data

// Content for the local demo. Sample friends answer each question with one of
// these lines, so the answers make sense when they unlock.

val SAMPLE_FRIENDS = listOf("Priya", "Sam", "Rohan", "Jaya", "Meera", "Arjun", "Kabir", "Zoya")

class DemoQuestion(val text: String, val answers: List<String>)

val DEMO_QUESTIONS = listOf(
    DemoQuestion(
        "What’s the worst gift you’ve ever received?",
        listOf(
            "A single sock. Not even a pair.",
            "A gym membership. From my ex. Two weeks after we broke up.",
            "A calendar from 2019. In 2021.",
            "A framed photo of the person who gave it to me.",
            "Soap shaped like a fish. It still scares me.",
        ),
    ),
    DemoQuestion(
        "If you could only eat one food forever, what would it be?",
        listOf(
            "Biryani. No further questions.",
            "Pizza, and I’d be happy about it.",
            "Mum’s dal chawal. Comfort forever.",
            "Masala dosa, extra chutney.",
            "Momos. Steamed, then fried, then steamed again.",
        ),
    ),
    DemoQuestion(
        "What’s a skill you pretend to have?",
        listOf(
            "Knowing what a carburetor does.",
            "Understanding wine. I just say “oaky.”",
            "Parallel parking. I leave and come back later.",
            "Reading maps. I follow the blue dot and pray.",
            "Being a morning person.",
        ),
    ),
    DemoQuestion(
        "What’s your most controversial food opinion?",
        listOf(
            "Pineapple on pizza is elite.",
            "Ketchup on Maggi. Fight me.",
            "Gulab jamun is better cold.",
            "Cake is mostly overrated.",
            "Chai should never be sugar-free.",
        ),
    ),
    DemoQuestion(
        "What would your autobiography be called?",
        listOf(
            "Five More Minutes",
            "I Was Told There Would Be Snacks",
            "Typing…",
            "Running Late, Again",
            "It Made Sense at the Time",
        ),
    ),
    DemoQuestion(
        "Which fictional world would you move to tomorrow?",
        listOf(
            "Hogwarts, obviously. Hufflepuff and proud.",
            "Wakanda, for the tech alone.",
            "The Shire. Second breakfast is a lifestyle.",
            "Anywhere in a Studio Ghibli film.",
            "Pandora, minus the war.",
        ),
    ),
    DemoQuestion(
        "What’s your go-to karaoke song?",
        listOf(
            "Bohemian Rhapsody, all parts myself.",
            "Anything by Arijit, with full drama.",
            "Wannabe. Every time.",
            "Don’t Stop Believin’. Group effort.",
            "I just hold the mic and dance.",
        ),
    ),
    DemoQuestion(
        "What’s a small thing that instantly makes your day?",
        listOf(
            "When the chai is exactly the right temperature.",
            "A dog noticing me on the street.",
            "Finding money in an old jacket.",
            "Green lights all the way home.",
            "When this group answers early.",
        ),
    ),
    DemoQuestion(
        "If you had to teach a class on anything, what would it be?",
        listOf(
            "How to nap anywhere.",
            "Advanced procrastination.",
            "Making the perfect maggi.",
            "Bargaining at street markets.",
            "How to win any argument with a friend.",
        ),
    ),
    DemoQuestion(
        "What’s a hill you will die on?",
        listOf(
            "Socks with sandals are comfortable.",
            "The window seat is the best seat.",
            "Voice notes over two minutes should be illegal.",
            "Breakfast for dinner is a valid dinner.",
            "Dark mode, always.",
        ),
    ),
    DemoQuestion(
        "What’s the best meal you’ve ever had?",
        listOf(
            "Street-side vada pav in the rain.",
            "My grandmother’s Sunday lunch.",
            "Midnight paratha after a road trip.",
            "Fresh fish on a beach in Goa.",
            "Hostel instant noodles at 3 AM. Peak cuisine.",
        ),
    ),
    DemoQuestion(
        "What did you want to be when you were seven?",
        listOf(
            "An astronaut. Still a little bit.",
            "A cricket captain.",
            "A shopkeeper, purely for the calculator.",
            "A dinosaur. Not a scientist, a dinosaur.",
            "A teacher, so I could hold the chalk.",
        ),
    ),
    DemoQuestion(
        "What’s the pettiest thing you’ve ever done?",
        listOf(
            "Left someone on read for exactly a week.",
            "Unfollowed someone over a cricket opinion.",
            "Ate the last samosa on purpose.",
            "Changed the Wi-Fi password and didn’t tell my brother.",
            "Kept a list. I still have the list.",
        ),
    ),
    DemoQuestion(
        "Which animal would you be, and why?",
        listOf(
            "A cat. Naps, snacks, judging people.",
            "An otter, holding hands while sleeping.",
            "A dolphin. Smart and always on holiday.",
            "A sloth. No explanation needed.",
            "A golden retriever. Just happy to be here.",
        ),
    ),
    DemoQuestion(
        "What’s the best thing you bought this year?",
        listOf(
            "Noise-cancelling headphones. Life-changing.",
            "A really good pillow.",
            "A plant that’s somehow still alive.",
            "Concert tickets. Zero regrets.",
            "A water bottle I actually use.",
        ),
    ),
    DemoQuestion(
        "Who here would survive a zombie apocalypse the longest?",
        listOf(
            "Priya. She’d have a spreadsheet for it.",
            "Not me. I’d go back for my phone charger.",
            "Rohan. He never leaves the house anyway.",
            "Whoever has the most snacks saved.",
            "Me, because I’d hide and nap.",
        ),
    ),
    DemoQuestion(
        "What’s your comfort show?",
        listOf(
            "Friends, for the 40th time.",
            "The Office. Season 3 forever.",
            "Panchayat. Pure peace.",
            "Old cricket highlights.",
            "Brooklyn Nine-Nine. Noice.",
        ),
    ),
    DemoQuestion(
        "What’s the worst haircut you’ve ever had?",
        listOf(
            "The bowl cut. There are photos.",
            "I asked for a trim and got a buzz.",
            "Self-cut during lockdown. Never again.",
            "Spiky with too much gel, 2009.",
            "The salon called it “edgy.” It wasn’t.",
        ),
    ),
    DemoQuestion(
        "What’s a sound you love?",
        listOf(
            "Rain on a tin roof.",
            "The pressure cooker whistle. Means food.",
            "A cricket ball hitting the middle of the bat.",
            "Waves at night.",
            "My phone not ringing on a Sunday.",
        ),
    ),
    DemoQuestion(
        "What’s your ideal Sunday?",
        listOf(
            "Sleep till noon, then brunch.",
            "Long drive, loud music, good food.",
            "Doing absolutely nothing, guilt-free.",
            "Cricket with friends, then chai.",
            "Movie marathon in pyjamas.",
        ),
    ),
    DemoQuestion(
        "What’s a word or phrase you say way too much?",
        listOf(
            "“Basically.”",
            "“Five minutes.” It’s never five minutes.",
            "“Wait, what?”",
            "“Literally.”",
            "“Chalo, let’s go.”",
        ),
    ),
    DemoQuestion(
        "What’s your signature dish?",
        listOf(
            "Maggi, but fancy.",
            "Egg bhurji. Undefeated.",
            "Ordering food very confidently.",
            "Chai. It counts.",
            "Pasta with whatever’s in the fridge.",
        ),
    ),
    DemoQuestion(
        "What’s a tiny luxury you refuse to give up?",
        listOf(
            "Good coffee every morning.",
            "Fresh bedsheets on Sundays.",
            "Taking a cab when it rains.",
            "One dessert a day, minimum.",
            "Fancy socks.",
        ),
    ),
    DemoQuestion(
        "What’s your most irrational fear?",
        listOf(
            "Lizards. Small, but plotting.",
            "Sending a text to the wrong person.",
            "Escalators eating my shoelaces.",
            "Answering phone calls from unknown numbers.",
            "Deep water I can’t see into.",
        ),
    ),
    DemoQuestion(
        "If you opened a café, what would it be called?",
        listOf(
            "Brew-tiful Day",
            "Chai Wala 2.0",
            "Bean There",
            "The Daily Question Café",
            "Mug Life",
        ),
    ),
    DemoQuestion(
        "What’s the most spontaneous thing you’ve ever done?",
        listOf(
            "Booked a trip to Goa the same night.",
            "Cut my own fringe at midnight.",
            "Adopted a cat on a Tuesday.",
            "Quit a job by text. Don’t recommend.",
            "Said yes to a road trip with zero plan.",
        ),
    ),
    DemoQuestion(
        "What’s the best compliment you’ve ever received?",
        listOf(
            "“You make everyone feel welcome.”",
            "“Your playlist is perfect.”",
            "A kid told me I was “very tall and funny.”",
            "“You give the best advice.”",
            "“You haven’t changed at all.” In a good way.",
        ),
    ),
    DemoQuestion(
        "What would you do with an extra hour every day?",
        listOf(
            "Sleep. Obviously sleep.",
            "Finally learn guitar.",
            "Cook proper breakfast.",
            "Call my grandparents more.",
            "Read the books piling up on my shelf.",
        ),
    ),
    DemoQuestion(
        "What’s a small win from today?",
        listOf(
            "Drank enough water for once.",
            "Got a seat on the metro.",
            "Inbox at zero for ten whole minutes.",
            "Made it to the gym.",
            "Answered this before everyone else.",
        ),
    ),
    DemoQuestion(
        "What’s something you’re looking forward to?",
        listOf(
            "The long weekend.",
            "My cousin’s wedding. The food, mainly.",
            "Winter and blankets.",
            "Seeing all of you soon.",
            "The next season of my show.",
        ),
    ),
)

val SAMPLE_REPLIES = listOf(
    "Haha, this is so you.",
    "Okay, this one wins today.",
    "I can’t stop laughing.",
    "Wait, I need the full story.",
    "Same, honestly.",
    "Never change.",
    "This is the best answer here.",
    "No way, really?",
)

/** Deterministic shuffle: each group walks through the questions in its own order. */
fun questionIndexFor(seed: Int, day: Int): Int {
    val n = DEMO_QUESTIONS.size
    val order = IntArray(n) { it }
    val rand = Mulberry32(seed + (day - 1) / n)
    for (i in n - 1 downTo 1) {
        val j = (rand.next() * (i + 1)).toInt()
        val t = order[i]; order[i] = order[j]; order[j] = t
    }
    return order[(day - 1) % n]
}

/** Same small random generator as the earlier app, so a seed gives the same order. */
class Mulberry32(private var state: Int) {
    fun next(): Double {
        state += 0x6D2B79F5
        var t = state
        t = (t xor (t ushr 15)) * (1 or t)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }
}
