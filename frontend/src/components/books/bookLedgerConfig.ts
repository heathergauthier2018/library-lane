import ratedBookIcon from "../../assets/storybook/icons/Rated Book.png";
import rereadIcon from "../../assets/storybook/icons/reread-rating.png";
import devastatedIcon from "../../assets/storybook/icons/emotionally-devastated-rating.png";
import romanceIcon from "../../assets/storybook/icons/romance-rating.png";
import spicyIcon from "../../assets/storybook/icons/spicy-rating.png";
import horrorIcon from "../../assets/storybook/icons/horror-rating.png";
import excitementIcon from "../../assets/storybook/icons/excitement-rating.png";

export type NewBook = {
  title: string;
  author: string;
  genre: string;
  format: string;
  readingStatus: string;
  [key: string]: string;
};

export type Option = {
  label: string;
  value: string;
};

export type RatingIcon =
  | "overall"
  | "reread"
  | "devastated"
  | "romance"
  | "spice"
  | "horror"
  | "excitement";

export type FieldType = "input" | "textarea" | "dropdown" | "rating" | "longPrompt";

export type FieldConfig = {
  key: string;
  label: string;
  type?: FieldType;
  placeholder?: string;
  rows?: number;
  options?: Option[];
  ratingIcon?: RatingIcon;
  showIf?: (book: NewBook) => boolean;
};

export type LedgerSide = {
  eyebrow: string;
  title: string;
  fields: FieldConfig[];
};

export type LedgerPage = {
  id: string;
  left: LedgerSide;
  right: LedgerSide;
};

export const formatOptions: Option[] = [
  { label: "Physical Book", value: "PHYSICAL" },
  { label: "E-Book", value: "EBOOK" },
  { label: "Audiobook", value: "AUDIOBOOK" },
  { label: "Mixed Formats", value: "MIXED" },
];

export const statusOptions: Option[] = [
  { label: "Want To Read", value: "TO_READ" },
  { label: "Currently Reading", value: "READING" },
  { label: "Completed", value: "COMPLETED" },
  { label: "Did Not Finish", value: "DNF" },
];

export const yesNoOptions: Option[] = [
  { label: "Unknown", value: "" },
  { label: "No", value: "NO" },
  { label: "Yes", value: "YES" },
];

export const ratingIcons: Record<RatingIcon, string> = {
  overall: ratedBookIcon,
  reread: rereadIcon,
  devastated: devastatedIcon,
  romance: romanceIcon,
  spice: spicyIcon,
  horror: horrorIcon,
  excitement: excitementIcon,
};

export function f(
  key: string,
  label: string,
  type: FieldType = "input",
  placeholder = "",
  extra: Partial<FieldConfig> = {}
): FieldConfig {
  return { key, label, type, placeholder, ...extra };
}

export const normalizeText = (value: string) =>
  value.trim().toLowerCase().replace(/\s+/g, " ");

export const genreIncludes = (book: NewBook, words: string[]) => {
  const genre = normalizeText(book.genre || "");
  return words.some((word) => genre.includes(normalizeText(word)));
};

export const hookStatusOptions: Option[] = [
  { label: "Not selected", value: "" },
  { label: "Yes", value: "YES" },
  { label: "No", value: "NO" },
  { label: "Not sure yet", value: "NOT_SURE" },
];

export const recommendOptions: Option[] = [
  { label: "Not selected", value: "" },
  { label: "Yes", value: "YES" },
  { label: "No", value: "NO" },
  { label: "With warnings", value: "WITH_WARNINGS" },
  { label: "Only to certain readers", value: "CERTAIN_READERS" },
];

export const publisherOptions: Option[] = [
  { label: "Not selected", value: "" },
  { label: "Penguin Random House", value: "PENGUIN_RANDOM_HOUSE" },
  { label: "HarperCollins", value: "HARPERCOLLINS" },
  { label: "Simon & Schuster", value: "SIMON_AND_SCHUSTER" },
  { label: "Macmillan", value: "MACMILLAN" },
  { label: "Hachette", value: "HACHETTE" },
  { label: "Scholastic", value: "SCHOLASTIC" },
  { label: "Bloomsbury", value: "BLOOMSBURY" },
  { label: "Tor", value: "TOR" },
  { label: "Orbit", value: "ORBIT" },
  { label: "Sourcebooks", value: "SOURCEBOOKS" },
  { label: "Entangled", value: "ENTANGLED" },
  { label: "Berkley", value: "BERKLEY" },
  { label: "Wednesday Books", value: "WEDNESDAY_BOOKS" },
  { label: "Del Rey", value: "DEL_REY" },
  { label: "Avon", value: "AVON" },
  { label: "Indie / Self-Published", value: "INDIE_SELF_PUBLISHED" },
  { label: "Other", value: "OTHER" },
];

export const ownedOptions: Option[] = [
  { label: "Not selected", value: "" },
  { label: "Owned", value: "OWNED" },
  { label: "Borrowed", value: "BORROWED" },
  { label: "Library Copy", value: "LIBRARY_COPY" },
  { label: "Kindle Unlimited", value: "KINDLE_UNLIMITED" },
  { label: "Audible", value: "AUDIBLE" },
  { label: "Gifted", value: "GIFTED" },
];

export const sourceOptions: Option[] = [
  { label: "Not selected", value: "" },
  { label: "Library", value: "LIBRARY" },
  { label: "Bookstore", value: "BOOKSTORE" },
  { label: "Online", value: "ONLINE" },
  { label: "Gift", value: "GIFT" },
  { label: "Book Box", value: "BOOK_BOX" },
  { label: "Friend", value: "FRIEND" },
  { label: "School / Class", value: "SCHOOL" },
  { label: "Other", value: "OTHER" },
];

export const romancePresenceOptions: Option[] = [
  { label: "No romance", value: "NO" },
  { label: "Minor romance", value: "MINOR" },
  { label: "Side romance", value: "SIDE" },
  { label: "Significant romance", value: "SIGNIFICANT" },
  { label: "Primary romance plot", value: "PRIMARY" },
];

export const romanceImportanceOptions: Option[] = [
  { label: "Background", value: "BACKGROUND" },
  { label: "Supporting Plot", value: "SUPPORTING" },
  { label: "Major Plot", value: "MAJOR" },
  { label: "Entire Story", value: "ENTIRE_STORY" },
];

export const readingModeOptions: Option[] = [
  { label: "Not selected", value: "" },
  { label: "Solo Read", value: "SOLO" },
  { label: "Buddy Read", value: "BUDDY_READ" },
  { label: "Book Club", value: "BOOK_CLUB" },
  { label: "Class / School", value: "CLASS" },
  { label: "ARC / Early Copy", value: "ARC" },
  { label: "Reread", value: "REREAD" },
];

export const atmosphereOptions: Option[] = [
  { label: "Not selected", value: "" },
  { label: "Cozy", value: "COZY" },
  { label: "Magical", value: "MAGICAL" },
  { label: "Romantic", value: "ROMANTIC" },
  { label: "Haunting", value: "HAUNTING" },
  { label: "Whimsical", value: "WHIMSICAL" },
  { label: "Dark Academia", value: "DARK_ACADEMIA" },
  { label: "Gothic", value: "GOTHIC" },
  { label: "Adventure", value: "ADVENTURE" },
  { label: "Historical", value: "HISTORICAL" },
  { label: "Mysterious", value: "MYSTERIOUS" },
];

export const isSeries = (book: NewBook) => book.isSeries === "YES";

export const hasRomance = (book: NewBook) =>
  book.romancePresence !== "" && book.romancePresence !== "NO";

export const hasScareGenre = (book: NewBook) =>
  genreIncludes(book, [
    "horror",
    "thriller",
    "gothic",
    "paranormal",
    "true crime",
    "dark academia",
    "suspense",
  ]);

export const isAudio = (book: NewBook) =>
  book.format === "AUDIOBOOK" || book.format === "MIXED";

export const isPageBased = (book: NewBook) =>
  book.format === "PHYSICAL" ||
  book.format === "EBOOK" ||
  book.format === "MIXED";

export const basePages: LedgerPage[] = [
  {
    id: "story-identity",
    left: {
      eyebrow: "Library Lane Ledger",
      title: "Add a New Story",
      fields: [
        f("title", "Story Title *", "input", "e.g. Fourth Wing"),
        f("author", "Author", "input", "e.g. Rebecca Yarros"),
        f("genre", "Genre", "input", "Fantasy romance, gothic, cozy mystery..."),
        f("format", "Format *", "dropdown", "", { options: formatOptions }),
        f("readingStatus", "Reading Status *", "dropdown", "", {
          options: statusOptions,
        }),
        f("isSeries", "Is This Part Of A Series?", "dropdown", "", {
          options: yesNoOptions,
        }),
      ],
    },
    right: {
      eyebrow: "Story Snapshot",
      title: "Book Details",
      fields: [
        f("seriesName", "Series Name", "input", "e.g. The Empyrean", {
          showIf: isSeries,
        }),
        f("seriesNumber", "Book Number", "input", "e.g. 1", {
          showIf: isSeries,
        }),
        f("publicationYear", "Publish Year", "input", "e.g. 2023"),
        f("editionFormat", "Edition", "input", "Hardcover, paperback, sprayed edges..."),
        f("publisher", "Publisher", "dropdown", "", { options: publisherOptions }),
        f("publisherOther", "Other Publisher", "input", "Type publisher name...", {
          showIf: (book) => book.publisher === "OTHER",
        }),
        f("pageCount", "Total Pages", "input", "e.g. 450", {
          showIf: isPageBased,
        }),
        f("audioLength", "Audiobook Length", "input", "e.g. 18h 22m", {
          showIf: isAudio,
        }),
        f("narrator", "Narrated By", "input", "e.g. Rebecca Soler", {
          showIf: isAudio,
        }),
        f(
          "description",
          "A Glimpse Inside",
          "textarea",
          "The publisher's story description…",
          { rows: 4 }
        ),
      ],
    },
  },
  {
    id: "reading-journey",
    left: {
      eyebrow: "Reading Journey",
      title: "Progress & Pace",
      fields: [
        f("currentPage", "Current Page", "input", "e.g. 125", {
          showIf: isPageBased,
        }),
        f("currentListeningPosition", "Current Listening Position", "input", "e.g. 4h 10m", {
          showIf: isAudio,
        }),
        f("listeningSpeed", "Listening Speed", "input", "e.g. 1.25", {
          showIf: isAudio,
        }),
        f("startDate", "Start Date", "input", "June 14, 2026"),
        f("finishDate", "Finish Date", "input", "June 21, 2026"),
      ],
    },
    right: {
      eyebrow: "Reading Rhythm",
      title: "How I Read It",
      fields: [
        f("readingPace", "Reading Pace", "longPrompt", "Binge read, slow read, savored over time..."),
        f("readingBreaks", "Reading Breaks", "longPrompt", "Did you stop, restart, pause, or read straight through?"),
        f("usualReadingTime", "Usual Reading Time", "input", "Morning, late night, commute..."),
        f("usualReadingPlace", "Where Did You Read Most?", "input", "Bed, couch, library, coffee shop..."),
        f("readingMode", "How Are You Reading It?", "dropdown", "", {
          options: readingModeOptions,
        }),
        f("readingGoal", "Reading Goal This Counts Toward", "input", "2026 goal, series goal, genre goal..."),
      ],
    },
  },
  {
    id: "before-the-journey",
    left: {
      eyebrow: "Before The Journey",
      title: "First Expectations",
      fields: [
        f("excitementRating", "Excitement Rating", "rating", "", {
          ratingIcon: "excitement",
        }),
        f("predictedRating", "Predicted Rating", "rating", "", {
          ratingIcon: "overall",
        }),
        f("whyExcited", "Why Are You Excited To Start?", "longPrompt", "What makes you want to open this book right now?"),
        f("whatYouHopeItGives", "What Are You Hoping It Gives You?", "longPrompt", "Comfort, escape, romance, mystery, knowledge, magic..."),
        f("whatYouAreWorriedAbout", "What Are You Worried It Might Do?", "longPrompt", "Bad pacing, disappointment, overhype, too much emotional damage..."),
      ],
    },
    right: {
      eyebrow: "Before The First Page",
      title: "First Impressions",
      fields: [
        f("readingMood", "Reading Mood", "input", "Cozy, curious, emotional, adventurous..."),
        f("coverFirstImpression", "Cover First Impression", "longPrompt", "What did the cover, title, or vibe make you expect?"),
        f("whatMakesYouThinkYouWillLoveIt", "What Makes You Think You’ll Love It?", "longPrompt", "A trope, setting, author, premise, character type, or vibe..."),
        f("premisePrediction", "Premise Prediction", "longPrompt", "Based on the title, cover, blurb, or vibes, what do you think this story is about?"),
      ],
    },
  },
  {
    id: "predictions",
    left: {
      eyebrow: "Story Instincts",
      title: "What I Think Might Happen",
      fields: [
        f("endingPrediction", "Ending Prediction", "longPrompt", "What do you think might happen by the end?"),
        f("plotPrediction", "Biggest Plot Prediction", "longPrompt", "What twist, reveal, or conflict do you expect?"),
        f("whatFeelsImportant", "What Feels Important Already?", "input", "A symbol, object, character, place, phrase, or detail..."),
        f("twistYouAreBracingFor", "What Twist Are You Bracing For?", "longPrompt", "What are you suspicious might happen?"),
      ],
    },
    right: {
      eyebrow: "First Hunches",
      title: "Who & What I’m Watching",
      fields: [
        f("characterPredictionLove", "Character I Think I’ll Love", "input", "If you already know one, who has your attention?"),
        f("characterPredictionConcern", "Character I’m Unsure About", "input", "Anyone suspicious, messy, or questionable?"),
        f("whatFeelsLikeAClue", "What Feels Like A Clue?", "input", "A key, letter, repeated phrase, color, object, or odd detail..."),
        f("predictionConfidence", "How Confident Are You?", "input", "Completely guessing, suspiciously confident, no clue..."),
      ],
    },
  },
  {
    id: "romance-content-notes",
    left: {
      eyebrow: "Reader Care",
      title: "Romance & Ratings",
      fields: [
        f("romancePresence", "Is Romance Present?", "dropdown", "", {
          options: romancePresenceOptions,
        }),
        f("romanceImportance", "Romance Importance", "dropdown", "", {
          options: romanceImportanceOptions,
          showIf: hasRomance,
        }),
        f("spiceRating", "Spice Rating", "rating", "", {
          ratingIcon: "spice",
          showIf: hasRomance,
        }),
        f("romanceRating", "Romance Rating", "rating", "", {
          ratingIcon: "romance",
          showIf: hasRomance,
        }),
        f("romanceNotes", "Romance Notes", "longPrompt", "Was it sweet, intense, subtle, messy, central, or barely there?", {
          showIf: hasRomance,
        }),
      ],
    },
    right: {
      eyebrow: "Content Notes",
      title: "Reader Cautions",
      fields: [
        f("triggerWarnings", "Trigger Warnings", "longPrompt", "Optional notes for difficult content..."),
        f("contentNotes", "Content Notes", "longPrompt", "Violence, grief, war, addiction, mental health, loss..."),
        f("violenceLevel", "Violence Level", "longPrompt", "None, mild, moderate, graphic..."),
        f("emotionalHeaviness", "Emotional Heaviness", "longPrompt", "Light, moderate, heavy, devastating..."),
        f("readerCautionNotes", "Reader Caution Notes", "longPrompt", "Anything you’d want another reader to know first..."),
      ],
    },
  },
  {
    id: "during-the-journey",
    left: {
      eyebrow: "During The Journey",
      title: "While Reading",
      fields: [
        f("currentRating", "Current Rating", "rating", "", {
          ratingIcon: "overall",
        }),
        f("currentExcitementRating", "Excitement While Reading", "rating", "", {
          ratingIcon: "excitement",
        }),
        f("firstSurprise", "Biggest Surprise So Far", "longPrompt", "What caught your attention or happened earlier than expected?"),
        f("hasBookHookedYou", "Has The Book Hooked You Yet?", "dropdown", "", {
          options: hookStatusOptions,
        }),
        f("whatHookedYou", "What Hooked You?", "longPrompt", "Was it a chapter, scene, quote, character moment, or twist?", {
          showIf: (book) => book.hasBookHookedYou === "YES",
        }),
      ],
    },
    right: {
      eyebrow: "Reading Check-In",
      title: "So Far",
      fields: [
        f("favoriteCharacterSoFar", "Favorite Character So Far", "longPrompt", "Who is winning you over, and why?"),
        f("biggestQuestionSoFar", "Biggest Question So Far", "longPrompt", "What are you trying to figure out?"),
        f("consideredDnf", "Have You Considered DNFing?", "input", "No, briefly, almost, yes..."),
        f("isPageTurner", "Is This A Page-Turner?", "dropdown", "", {
          options: hookStatusOptions,
        }),
        f("pageTurnerWhy", "What Makes It A Page-Turner?", "longPrompt", "Is it the mystery, romance, stakes, pacing, writing, or world?", {
          showIf: (book) => book.isPageTurner === "YES",
        }),
        f("currentTheory", "Current Theory", "longPrompt", "What do you think is really going on?"),
      ],
    },
  },
  {
    id: "characters-world",
    left: {
      eyebrow: "Characters & World",
      title: "People I Met",
      fields: [
        f("favoriteCharacter", "Favorite Character", "longPrompt", "Who did you love most, and why?"),
        f("relatableCharacter", "Most Relatable Character", "longPrompt", "Who felt familiar or personal to you?"),
        f("mostComplexCharacter", "Most Complex Character", "longPrompt", "Who had the most layers?"),
        f("characterYouWouldBefriend", "Character You’d Befriend", "longPrompt", "Who would you want beside you, and why?"),
        f("characterToAvoid", "Character You’d Avoid", "longPrompt", "Who would you run from, and why?"),
        f("whoDeservedBetter", "Who Deserved Better?", "longPrompt", "Who needed a kinder ending?"),
        f("characterWhoChangedYourMind", "Character Who Changed Your Mind", "longPrompt", "Who surprised you?"),
      ],
    },
    right: {
      eyebrow: "World Notes",
      title: "Places & People",
      fields: [
        f("characterGrowth", "Biggest Character Growth", "longPrompt", "Who changed the most?"),
        f("favoriteRelationship", "Most Memorable Relationship", "longPrompt", "Friendship, romance, sibling bond, found family..."),
        f("favoriteVillain", "Favorite Villain", "longPrompt", "Who made the story dangerous?"),
        f("favoriteSideCharacter", "Favorite Side Character", "longPrompt", "Who stole the scene?"),
        f("favoriteLocation", "Favorite Location", "longPrompt", "Kingdom, school, city, forest, spaceship..."),
        f("placeToVisit", "Place You’d Visit", "longPrompt", "Where would you go?"),
        f("placeToAvoid", "Place You’d Never Visit", "longPrompt", "Where would you absolutely avoid?"),
        f("worldbuildingNotes", "Best Worldbuilding Detail", "longPrompt", "What made this world feel real?"),
        f("loreNotes", "Most Interesting Lore", "longPrompt", "What history, myth, rule, or detail stayed with you?"),
      ],
    },
  },
  {
    id: "quotes-memories",
    left: {
      eyebrow: "Memory Vault",
      title: "Collected Lines",
      fields: [
        f("favoriteQuote", "Favorite Quote", "longPrompt", "A line that stayed with you..."),
        f("wallQuote", "Quote I’d Put On A Wall", "longPrompt", "What line would make a beautiful print, sticker, or keepsake?"),
        f("funnyQuote", "Funniest Quote", "longPrompt", "A line that made you laugh..."),
        f("heartbreakingQuote", "Heartbreaking Quote", "longPrompt", "A line that hurt..."),
        f("powerfulQuote", "Most Powerful Quote", "longPrompt", "A line that mattered..."),
      ],
    },
    right: {
      eyebrow: "Story Memories",
      title: "Moments To Keep",
      fields: [
        f("favoriteChapter", "Favorite Chapter", "input", "Chapter number, title, or moment..."),
        f("favoriteScene", "Favorite Scene", "longPrompt", "A scene you loved..."),
        f("sceneToFrame", "Scene I’d Frame", "longPrompt", "What moment would look beautiful as art?"),
        f("sceneThatBrokeMe", "Scene That Broke Me", "longPrompt", "The scene that emotionally wrecked you..."),
        f("movieScene", "Scene I’d Watch As A Movie", "longPrompt", "What would you love to see adapted?"),
        f("lineYouWouldKeep", "Line I’d Keep Forever", "longPrompt", "A sentence you wish you could carry..."),
      ],
    },
  },
  {
    id: "tropes-themes",
    left: {
      eyebrow: "Tropes & Themes",
      title: "Story Patterns",
      fields: [
        f("tropesIncluded", "Tropes Included", "longPrompt", "Found family, enemies to lovers, chosen one..."),
        f("favoriteTrope", "Favorite Trope", "input", "Which trope worked best?"),
        f("leastFavoriteTrope", "Least Favorite Trope", "input", "Was there one that did not work?"),
        f("tropeThatSurprisedMe", "Trope That Surprised Me", "input", "Which trope caught you off guard?"),
        f("tropeWishlist", "Trope You Wanted More Of", "input", "Was there one you wanted more of?"),
      ],
    },
    right: {
      eyebrow: "Theme Notes",
      title: "What It Explored",
      fields: [
        f("tropeExecution", "Trope Execution", "longPrompt", "Did the story handle its tropes well?"),
        f("didSubvertExpectations", "Did It Subvert Expectations?", "longPrompt", "Did it twist or deepen something familiar?"),
        f("themesExplored", "Themes Explored", "longPrompt", "Grief, courage, power, friendship, survival..."),
        f("themeConnectedWith", "Theme I Connected With", "longPrompt", "What theme felt personal?"),
        f("themeStillThinkingAbout", "Theme Still On My Mind", "longPrompt", "What idea is still sitting with you?"),
      ],
    },
  },
  {
    id: "emotional-journey",
    left: {
      eyebrow: "Emotional Journey",
      title: "How It Felt",
      fields: [
        f("emotionalDevastationRating", "Emotional Devastation Rating", "rating", "", {
          ratingIcon: "devastated",
        }),
        f("strongestEmotion", "Strongest Emotion", "input", "Joy, grief, wonder, dread, comfort..."),
        f("mostEmotionalMoment", "Most Emotional Moment", "longPrompt", "The moment that hit hardest..."),
        f("didYouCryLaughRage", "Did You Cry, Laugh, or Rage?", "longPrompt", "What emotions did this story pull out of you?"),
        f("mostTenseMoment", "Most Tense Moment", "longPrompt", "When were you most on edge?"),
      ],
    },
    right: {
      eyebrow: "After The Story",
      title: "The Feeling Left Behind",
      fields: [
        f("comfortingMoment", "Most Comforting Moment", "longPrompt", "Was there a moment that felt safe, warm, or healing?"),
        f("mostHopefulMoment", "Most Hopeful Moment", "longPrompt", "What moment gave you hope?"),
        f("moodAfterFinishing", "Mood After Finishing", "input", "Devastated, hopeful, peaceful, obsessed..."),
        f("bookHangoverLevel", "Book Hangover Level", "input", "None, mild, severe, emotionally unavailable..."),
        f("whatINeededAfter", "What I Needed After", "input", "A sequel, nap, hug, comedy, silence..."),
      ],
    },
  },
];

export const genrePages: Record<string, LedgerPage[]> = {
  fantasy: [
    {
      id: "fantasy-magic",
      left: {
        eyebrow: "Fantasy Ledger",
        title: "Magic & Destiny",
        fields: [
          f("fantasyMagicSystem", "Magic System", "longPrompt", "Soft, hard, mysterious, dangerous, costly?"),
          f("fantasyPowerYouWant", "Power You’d Want", "longPrompt", "Which magic would you choose, and why?"),
          f("fantasyCreature", "Favorite Creature", "longPrompt", "Dragon, fae, monster, familiar, beast..."),
          f("fantasyArtifact", "Magical Artifact", "longPrompt", "Weapon, ring, book, crown, relic..."),
          f("fantasyProphecy", "Prophecy / Lore", "longPrompt", "Did the prophecy, legend, or lore work?"),
        ],
      },
      right: {
        eyebrow: "Fantasy Ledger",
        title: "Kingdoms & Legends",
        fields: [
          f("fantasyKingdom", "Kingdom You’d Visit", "longPrompt", "Where would you go?"),
          f("fantasyEpicScene", "Most Epic Scene", "longPrompt", "Battle, reveal, betrayal, magical scene..."),
          f("fantasyFoundFamily", "Found Family Notes", "longPrompt", "Who became family?"),
          f("fantasyWorldDanger", "World Danger", "longPrompt", "What made this world risky, beautiful, or terrifying?"),
          f("fantasySpinoffCharacter", "Character Who Deserves A Spinoff", "longPrompt", "Who needs their own book?"),
        ],
      },
    },
  ],

    romantasy: [
    {
      id: "romantasy-heart-magic",
      left: {
        eyebrow: "Romantasy Ledger",
        title: "Heart & Magic",
        fields: [
          f("romantasyCouple", "Favorite Couple", "longPrompt", "Who owned the book?"),
          f("romantasyChemistry", "Chemistry Rating Notes", "longPrompt", "What made the chemistry work or not work?"),
          f("romantasyTension", "Best Romantic Tension", "longPrompt", "Where did the tension hit hardest?"),
          f("romantasyObstacle", "Magical Relationship Obstacle", "longPrompt", "War, curse, court politics, duty..."),
          f("romantasyProtectiveMoment", "Favorite Protective Moment", "longPrompt", "A rescue, shield, warning, sacrifice..."),
        ],
      },
      right: {
        eyebrow: "Romantasy Ledger",
        title: "Love & Power",
        fields: [
          f("romantasySeparation", "Most Devastating Separation", "longPrompt", "What almost broke them?"),
          f("romantasyConfession", "Favorite Confession", "longPrompt", "What line or moment hit hardest?"),
          f("romantasyWorldEffect", "Fantasy World Effect On Romance", "longPrompt", "Did the world make the love story stronger?"),
          f("romantasyPowerImbalance", "Power Imbalance", "longPrompt", "Did magic, status, duty, or destiny affect the relationship?"),
          f("romantasySacrifice", "Romantic Sacrifice", "longPrompt", "What did someone risk, give up, or protect for love?"),
          f("romantasyCourtPolitics", "Court / Kingdom Pressure", "longPrompt", "How did politics, war, family, or power shape the romance?"),
        ],
      },
    },
  ],

  romance: [
    {
      id: "romance-heart",
      left: {
        eyebrow: "Romance Ledger",
        title: "Falling In Love",
        fields: [
          f("romanceFavoriteMoment", "Favorite Romantic Moment", "longPrompt", "A look, confession, dance, quiet scene..."),
          f("romanceFavoriteDate", "Favorite Date", "longPrompt", "What moment felt sweet?"),
          f("romanceFavoriteKiss", "Favorite Kiss / Confession", "longPrompt", "What romantic scene hit hardest?"),
          f("romanceBanter", "Best Banter", "longPrompt", "What conversation made the chemistry work?"),
          f("romanceSlowBurn", "Slow Burn Rating Notes", "longPrompt", "Too slow, perfect tension, rushed, worth it?"),
        ],
      },
      right: {
        eyebrow: "Romance Ledger",
        title: "Relationship Dynamics",
        fields: [
          f("romanceCommunication", "Communication Rating Notes", "longPrompt", "Healthy, chaotic, refreshing, disastrous?"),
          f("romanceLoveInterest", "Favorite Love Interest", "longPrompt", "Who had your heart?"),
          f("romanceWouldDate", "Would You Date Them?", "longPrompt", "Yes, no, maybe, only in fiction..."),
          f("romanceEndingEarned", "Did They Earn The Ending?", "longPrompt", "Did the ending satisfy you?"),
          f("romanceFavoriteDynamic", "Favorite Relationship Dynamic", "longPrompt", "Slow burn, second chance, rivals, forbidden love, friends to lovers..."),
        ],
      },
    },
  ],

    paranormal: [
    {
      id: "paranormal-veil",
      left: {
        eyebrow: "Paranormal Ledger",
        title: "Strange & Unexplained",
        fields: [
          f("paranormalBeing", "Favorite Supernatural Being", "longPrompt", "Ghost, vampire, witch, demon, shifter..."),
          f("paranormalPower", "Favorite Power", "longPrompt", "What ability fascinated you?"),
          f("paranormalCreepiestMoment", "Creepiest Paranormal Moment", "longPrompt", "What chilled you?"),
          f("paranormalLore", "Lore You Wanted More Of", "longPrompt", "What mythology needed more space?"),
          f("paranormalHumanConflict", "Human / Supernatural Conflict", "longPrompt", "What tension came from crossing worlds?"),
        ],
      },
      right: {
        eyebrow: "Paranormal Ledger",
        title: "Hidden Worlds",
        fields: [
          f("paranormalImmortality", "Immortality Dilemma", "longPrompt", "Did immortality feel romantic, lonely, scary, or complicated?"),
          f("paranormalHiddenWorld", "Hidden World", "longPrompt", "How did the supernatural world work?"),
          f("paranormalHaunting", "Most Haunting Scene", "longPrompt", "What lingered after the page turned?"),
          f("paranormalRules", "Rules Of The Supernatural World", "longPrompt", "What rules governed the hidden world?"),
          f("paranormalKeepsake", "Paranormal Keepsake", "longPrompt", "What object best represents the supernatural side of the story?"),
        ],
      },
    },
  ],

  mystery: [
    {
      id: "mystery-case",
      left: {
        eyebrow: "Mystery Ledger",
        title: "The Investigation",
        fields: [
          f("mysteryFirstSuspect", "First Suspect", "longPrompt", "Who did you suspect first?"),
          f("mysteryFinalSuspect", "Final Suspect", "longPrompt", "Who did you suspect before the reveal?"),
          f("mysteryClueMissed", "Clue You Missed", "longPrompt", "What was hiding in plain sight?"),
          f("mysteryRedHerring", "Best Red Herring", "longPrompt", "What misled you?"),
          f("mysterySmartestCharacter", "Smartest Character", "longPrompt", "Who was paying attention?"),
        ],
      },
      right: {
        eyebrow: "Mystery Ledger",
        title: "Detective Notes",
        fields: [
          f("mysteryBiggestReveal", "Biggest Reveal", "longPrompt", "What changed everything?"),
          f("mysterySolvedIt", "Did You Solve It?", "longPrompt", "Did you guess the ending?"),
          f("mysteryEndingPayoff", "Ending Payoff", "longPrompt", "Did the ending feel fair and satisfying?"),
          f("mysterySuspiciousCharacter", "Most Suspicious Character", "longPrompt", "Who did you not trust?"),
          f("mysteryUnansweredQuestion", "Unanswered Question", "longPrompt", "What are you still wondering?"),
        ],
      },
    },
  ],

  cozyMystery: [
    {
      id: "cozy-mystery",
      left: {
        eyebrow: "Cozy Mystery Ledger",
        title: "Comfort & Clues",
        fields: [
          f("cozyMysterySetting", "Favorite Cozy Setting", "longPrompt", "Small town, bakery, bookshop, inn, library..."),
          f("cozyMysteryBusiness", "Local Business You’d Visit", "longPrompt", "Where would you stop first?"),
          f("cozyMysteryTeaCharacter", "Character You’d Have Tea With", "longPrompt", "Who would you sit with?"),
          f("cozyMysteryCommunity", "Best Community Moment", "longPrompt", "What made the town feel alive?"),
          f("cozyMysteryComfortObject", "Comfort Object", "longPrompt", "What item represents the cozy feeling of the story?"),
        ],
      },
      right: {
        eyebrow: "Cozy Mystery Ledger",
        title: "Charming Case Notes",
        fields: [
          f("cozyMysteryCharmingClue", "Most Charming Clue", "longPrompt", "What clue felt cozy, clever, or cute?"),
          f("cozyMysterySideCharacter", "Favorite Side Character", "longPrompt", "Who stole the scene?"),
          f("cozyMysteryComfortRating", "Comfort Rating Notes", "longPrompt", "How comforting was the mystery?"),
          f("cozyMysteryReveal", "Satisfying Reveal", "longPrompt", "Did the ending feel cozy and complete?"),
        ],
      },
    },
  ],

  thriller: [
    {
      id: "thriller-tension",
      left: {
        eyebrow: "Thriller Ledger",
        title: "Suspicion & Tension",
        fields: [
          f("thrillerStressfulChapter", "Most Stressful Chapter", "longPrompt", "Which chapter raised your heart rate?"),
          f("thrillerBetrayal", "Biggest Betrayal", "longPrompt", "Who turned the story upside down?"),
          f("thrillerTrustedTooMuch", "Character You Trusted Too Much", "longPrompt", "Who fooled you?"),
          f("thrillerJawDrop", "Jaw-Drop Moment", "longPrompt", "What made you stop reading?"),
          f("thrillerLie", "Biggest Lie", "longPrompt", "What deception carried the story?"),
        ],
      },
      right: {
        eyebrow: "Thriller Ledger",
        title: "Psychological Games",
        fields: [
          f("thrillerTwistRanking", "Twist Ranking", "longPrompt", "Weak, good, shocking, legendary..."),
          f("thrillerPacing", "Pacing Rating Notes", "longPrompt", "Slow burn, nonstop, uneven, perfect?"),
          f("thrillerManipulativeCharacter", "Most Manipulative Character", "longPrompt", "Who played everyone?"),
          f("thrillerEndingTension", "Ending Tension", "longPrompt", "Did the pressure hold until the last page?"),
          f("thrillerParanoiaLevel", "Paranoia Level", "longPrompt", "How suspicious did you become while reading?"),
        ],
      },
    },
  ],

  horror: [
    {
      id: "horror-fear",
      left: {
        eyebrow: "Horror Ledger",
        title: "Fear Ledger",
        fields: [
          f("horrorScariestScene", "Scariest Scene", "longPrompt", "What made your stomach drop?"),
          f("horrorDisturbingImage", "Most Disturbing Image", "longPrompt", "What visual stayed with you?"),
          f("horrorFearType", "Fear Type", "longPrompt", "Psychological, supernatural, body horror, gore..."),
          f("horrorVillainFear", "Monster / Villain Fear Rating Notes", "longPrompt", "Who or what frightened you most?"),
          f("horrorUnsettlingIdea", "Most Unsettling Idea", "longPrompt", "What concept stayed in your head?"),
        ],
      },
      right: {
        eyebrow: "Horror Ledger",
        title: "After Dark",
        fields: [
          f("horrorKeptAwake", "Did It Keep You Awake?", "longPrompt", "Yes, no, only that one scene..."),
          f("horrorPsychologicalNotes", "Psychological Horror Notes", "longPrompt", "What got under your skin?"),
          f("horrorGoreLevel", "Gore Level", "longPrompt", "None, mild, graphic, too much..."),
          f("horrorLingeringThought", "Lingering Thought", "longPrompt", "The idea, image, ending, character..."),
          f("horrorReadAtNight", "Would You Read It At Night?", "longPrompt", "Absolutely, never again, only with lights on..."),
        ],
      },
    },
  ],

  gothic: [
    {
      id: "gothic-shadows",
      left: {
        eyebrow: "Gothic Ledger",
        title: "Shadows & Secrets",
        fields: [
          f("gothicLocation", "Creepiest Location", "longPrompt", "House, moor, attic, ruins, corridor..."),
          f("gothicAtmosphere", "Haunted Atmosphere", "longPrompt", "What made it feel haunted?"),
          f("gothicTragicCharacter", "Tragic Character", "longPrompt", "Who carried the sadness?"),
          f("gothicFamilySecret", "Family Secret", "longPrompt", "What secret shaped the story?"),
          f("gothicSymbolism", "Symbolism", "longPrompt", "Objects, weather, house, ghosts, decay..."),
        ],
      },
      right: {
        eyebrow: "Gothic Ledger",
        title: "Beauty In Darkness",
        fields: [
          f("gothicDecay", "Decay / Ruin Imagery", "longPrompt", "What image captured the mood?"),
          f("gothicBeautifulDarkScene", "Most Beautiful Dark Scene", "longPrompt", "Where was the darkness gorgeous?"),
          f("gothicMysteryWalls", "Mystery In The Walls", "longPrompt", "What secret felt built into the setting?"),
          f("gothicEndingMood", "Ending Mood", "longPrompt", "Bleak, hopeful, haunted, unresolved..."),
        ],
      },
    },
  ],

  scifi: [
    {
      id: "scifi-future",
      left: {
        eyebrow: "Sci-Fi Ledger",
        title: "Future Visions",
        fields: [
          f("sciFiTechnology", "Favorite Technology", "longPrompt", "Ship, AI, invention, planet, system..."),
          f("sciFiTechnologyUse", "Invention You’d Use", "longPrompt", "What would you want in real life?"),
          f("sciFiTerrifyingTech", "Invention That Terrified You", "longPrompt", "What future possibility felt unsettling?"),
          f("sciFiEthicalDilemma", "Ethical Dilemma", "longPrompt", "What choice, invention, or society made you pause?"),
          f("sciFiSociety", "Planet / Ship / Society Notes", "longPrompt", "What made the setting work?"),
        ],
      },
      right: {
        eyebrow: "Sci-Fi Ledger",
        title: "Big Questions",
        fields: [
          f("sciFiFutureFear", "Future Fear", "longPrompt", "What did this future make you worry about?"),
          f("sciFiQuestion", "Big Question", "longPrompt", "What did this story make you wonder?"),
          f("sciFiConcept", "Scientific Concept", "longPrompt", "What concept felt smart or fascinating?"),
          f("sciFiWouldSurvive", "Would You Survive?", "longPrompt", "Absolutely, no chance, depends on the ship..."),
        ],
      },
    },
  ],

  dystopian: [
    {
      id: "dystopian-broken-worlds",
      left: {
        eyebrow: "Dystopian Ledger",
        title: "Broken Worlds",
        fields: [
          f("dystopianSystem", "System You’d Rebel Against", "longPrompt", "What rule would break you?"),
          f("dystopianRule", "Most Frightening Rule", "longPrompt", "What made the society terrifying?"),
          f("dystopianRealisticPart", "Most Realistic Part", "longPrompt", "What felt uncomfortably possible?"),
          f("dystopianResistance", "Resistance Moment", "longPrompt", "What moment pushed back against the system?"),
        ],
      },
      right: {
        eyebrow: "Dystopian Ledger",
        title: "Resistance",
        fields: [
          f("dystopianHopeCharacter", "Character Who Gave Hope", "longPrompt", "Who kept the light alive?"),
          f("dystopianSurvivalStrategy", "Survival Strategy", "longPrompt", "Hide, rebel, blend in, escape..."),
          f("dystopianSocialWarning", "Social Warning", "longPrompt", "What did this world warn about?"),
          f("dystopianMessage", "Most Powerful Message", "longPrompt", "What stayed with you?"),
        ],
      },
    },
  ],

  historical: [
    {
      id: "historical-time",
      left: {
        eyebrow: "Historical Ledger",
        title: "Through Time",
        fields: [
          f("historicalEvent", "Historical Event / Era", "longPrompt", "What event, era, or place did this story explore?"),
          f("historicalDetail", "Detail You Learned", "longPrompt", "What fact, custom, or detail stuck with you?"),
          f("historicalResearchPerson", "Person / Group To Research", "longPrompt", "Who do you want to learn more about?"),
          f("historicalSetting", "Setting Immersion", "longPrompt", "Did the setting feel real and lived in?"),
        ],
      },
      right: {
        eyebrow: "Historical Ledger",
        title: "Living History",
        fields: [
          f("historicalAccuracyNotes", "Historical Accuracy Notes", "longPrompt", "Researched, simplified, questionable, impressive?"),
          f("historicalEmotion", "Emotion Of The Era", "longPrompt", "Hope, fear, resilience, grief, rebellion..."),
          f("historicalSurprise", "What Surprised You?", "longPrompt", "What did you not expect?"),
          f("historicalResearch", "Further Research", "longPrompt", "Did this make you want to learn more?"),
        ],
      },
    },
  ],

  historicalRomance: [
    {
      id: "historical-romance",
      left: {
        eyebrow: "Historical Romance",
        title: "Love In Another Time",
        fields: [
          f("historicalRomanceCourtship", "Courtship Moment", "longPrompt", "What romantic gesture fit the era?"),
          f("historicalRomanceObstacle", "Historical Obstacle", "longPrompt", "Class, reputation, war, family, inheritance..."),
          f("historicalRomanceTension", "Best Tension", "longPrompt", "Where did the restraint or risk work best?"),
          f("historicalRomanceScandal", "Scandal Rating Notes", "longPrompt", "How scandalous was it for the time?"),
        ],
      },
      right: {
        eyebrow: "Historical Romance",
        title: "Rules & Romance",
        fields: [
          f("historicalRomanceRules", "Social Rules", "longPrompt", "What rules shaped the romance?"),
          f("historicalRomanceDetail", "Favorite Historical Detail", "longPrompt", "Clothing, setting, manners, events..."),
          f("historicalRomanceEraAppropriate", "Era-Appropriate Romance?", "longPrompt", "Did the romance feel true to the time?"),
          f("historicalRomanceEnding", "Ending Satisfaction", "longPrompt", "Did the ending feel earned?"),
        ],
      },
    },
  ],

    nonfiction: [
    {
      id: "nonfiction-learning",
      left: {
        eyebrow: "Nonfiction Ledger",
        title: "Learning Journey",
        fields: [
          f("nonfictionBiggestLearned", "Biggest Thing Learned", "longPrompt", "What did you learn?"),
          f("nonfictionFactShared", "Fact You Shared", "longPrompt", "What did you immediately tell someone?"),
          f("nonfictionChallengedIdea", "Idea That Challenged You", "longPrompt", "What made you rethink something?"),
          f("nonfictionStrongestArgument", "Strongest Argument", "longPrompt", "What point was most convincing?"),
        ],
      },
      right: {
        eyebrow: "Nonfiction Ledger",
        title: "Real World Impact",
        fields: [
          f("nonfictionUsefulChapter", "Most Useful Chapter", "longPrompt", "Which section mattered most?"),
          f("nonfictionChangedMind", "What Changed Your Mind?", "longPrompt", "Did your view shift?"),
          f("nonfictionApply", "What You’ll Apply", "longPrompt", "What will you use in real life?"),
          f("nonfictionWhoShouldRead", "Who Should Read It?", "longPrompt", "Who needs this book?"),
          f("nonfictionObjectLesson", "Object Lesson", "longPrompt", "What object, habit, note, or symbol could represent the main lesson?"),
        ],
      },
    },
  ],

  memoir: [
    {
      id: "memoir-life",
      left: {
        eyebrow: "Memoir / Biography",
        title: "Someone Else’s Life",
        fields: [
          f("memoirMovingMoment", "Most Moving Moment", "longPrompt", "What moment stayed with you?"),
          f("memoirHardestMoment", "Hardest Moment", "longPrompt", "What was difficult to read?"),
          f("memoirLifeLesson", "Biggest Life Lesson", "longPrompt", "What did their life teach you?"),
          f("memoirQuestion", "Question You’d Ask Them", "longPrompt", "If you could ask one thing..."),
        ],
      },
      right: {
        eyebrow: "Memoir / Biography",
        title: "Human Story",
        fields: [
          f("memoirChapterStayed", "Chapter That Stayed", "longPrompt", "Which part lingers?"),
          f("memoirAdmire", "What You Admire", "longPrompt", "What strength, honesty, or choice stood out?"),
          f("memoirSurprised", "What Surprised You?", "longPrompt", "What did you not expect?"),
          f("memoirLegacy", "Legacy Of The Person", "longPrompt", "What remains after reading their story?"),
          f("memoirKeepsake", "Keepsake From Their Story", "longPrompt", "What object, place, photo, song, or phrase represents their life?"),
        ],
      },
    },
  ],

  selfHelp: [
    {
      id: "self-help",
      left: {
        eyebrow: "Self-Help Ledger",
        title: "Useful Pages",
        fields: [
          f("selfHelpUsefulAdvice", "Most Useful Advice", "longPrompt", "What advice actually helped?"),
          f("selfHelpRejectedAdvice", "Advice You Rejected", "longPrompt", "What did not fit your life?"),
          f("selfHelpTryWeek", "Try This Week", "longPrompt", "What is one thing you could try soon?"),
          f("selfHelpTryMonth", "Try This Month", "longPrompt", "What is one bigger thing to practice?"),
        ],
      },
      right: {
        eyebrow: "Self-Help Ledger",
        title: "Practice Notes",
        fields: [
          f("selfHelpMindsetShift", "Mindset Shift", "longPrompt", "What changed in how you think?"),
          f("selfHelpQuote", "Quote To Remember", "longPrompt", "What line should stay with you?"),
          f("selfHelpHabit", "Habit To Build", "longPrompt", "What habit does this inspire?"),
          f("selfHelpPracticalRating", "Practical Rating Notes", "longPrompt", "How usable was the book?"),
          f("selfHelpTool", "Tool Or Ritual To Keep", "longPrompt", "A checklist, journal prompt, morning habit, mantra, or reminder..."),
        ],
      },
    },
  ],

  trueCrime: [
    {
      id: "true-crime-case",
      left: {
        eyebrow: "True Crime Ledger",
        title: "The Case File",
        fields: [
          f("trueCrimeShockingFact", "Most Shocking Fact", "longPrompt", "What detail stunned you?"),
          f("trueCrimeUnanswered", "Unanswered Question", "longPrompt", "What still feels unresolved?"),
          f("trueCrimeEvidence", "Strongest Evidence", "longPrompt", "What evidence mattered most?"),
          f("trueCrimeFrustrating", "Most Frustrating Part", "longPrompt", "What made you angry or unsettled?"),
        ],
      },
      right: {
        eyebrow: "True Crime Ledger",
        title: "Investigation Notes",
        fields: [
          f("trueCrimeVictimNotes", "Victim-Centered Notes", "longPrompt", "What should be remembered respectfully?"),
          f("trueCrimeJustice", "Justice Outcome", "longPrompt", "What happened legally or socially?"),
          f("trueCrimeMediaEthics", "Media Ethics Thoughts", "longPrompt", "Did the book handle the case responsibly?"),
          f("trueCrimeStayedWithMe", "Case Detail That Stayed", "longPrompt", "What will you not forget?"),
          f("trueCrimeCaseSymbol", "Case Symbol", "longPrompt", "What object, place, photo, document, or phrase represents this case?"),
        ],
      },
    },
  ],

  poetry: [
    {
      id: "poetry-lines",
      left: {
        eyebrow: "Poetry Ledger",
        title: "Lines & Language",
        fields: [
          f("poetryFavoritePoem", "Favorite Poem", "longPrompt", "Which poem stayed with you?"),
          f("poetryFavoriteLine", "Favorite Line", "longPrompt", "A line you want to keep..."),
          f("poetryStrongestImage", "Strongest Image", "longPrompt", "What image did the words create?"),
          f("poetryHitHardest", "Poem That Hit Hardest", "longPrompt", "Which one found you?"),
        ],
      },
      right: {
        eyebrow: "Poetry Ledger",
        title: "Reflection",
        fields: [
          f("poetryReread", "Poem To Reread", "longPrompt", "Which poem pulled you back?"),
          f("poetryEmotion", "Emotion Carried", "longPrompt", "What feeling moved through the collection?"),
          f("poetryMetaphor", "Favorite Metaphor", "longPrompt", "What comparison opened something up?"),
          f("poetryMemorize", "Line You’d Memorize", "longPrompt", "What line belongs in your memory?"),
          f("poetryColor", "Color Of The Collection", "longPrompt", "If these poems had a color palette, what would it be?"),
        ],
      },
    },
  ],

  contemporary: [
    {
      id: "contemporary-literary",
      left: {
        eyebrow: "Contemporary Ledger",
        title: "Human Moments",
        fields: [
          f("contemporaryHumanMoment", "Most Human Moment", "longPrompt", "What felt painfully or beautifully real?"),
          f("contemporaryConflict", "Most Realistic Conflict", "longPrompt", "What conflict felt true?"),
          f("contemporaryRealCharacter", "Character Who Felt Real", "longPrompt", "Who felt like an actual person?"),
          f("contemporaryQuestion", "Question It Left", "longPrompt", "What question stayed with you?"),
        ],
      },
      right: {
        eyebrow: "Literary Ledger",
        title: "Quiet Meaning",
        fields: [
          f("contemporaryQuietScene", "Quiet Scene That Mattered", "longPrompt", "What small moment carried weight?"),
          f("contemporaryTheme", "Theme That Lingered", "longPrompt", "What idea stayed alive after finishing?"),
          f("contemporaryWritingStyle", "Writing Style Notes", "longPrompt", "Sparse, lyrical, sharp, intimate, strange..."),
          f("contemporaryEnding", "Ending Interpretation", "longPrompt", "What do you think the ending meant?"),
          f("contemporaryEverydayObject", "Everyday Object That Mattered", "longPrompt", "What ordinary item carried meaning?"),
        ],
      },
    },
  ],

  magicalRealism: [
    {
      id: "magical-realism",
      left: {
        eyebrow: "Magical Realism",
        title: "Ordinary Magic",
        fields: [
          f("magicalRealismElement", "Magical Element", "longPrompt", "What impossible thing existed quietly?"),
          f("magicalRealismSymbol", "What It Symbolized", "longPrompt", "What did the magic mean?"),
          f("magicalRealismBlur", "Moment Reality Blurred", "longPrompt", "When did ordinary and impossible mix?"),
          f("magicalRealismInterpretation", "Interpretation", "longPrompt", "How did you read the magic?"),
        ],
      },
      right: {
        eyebrow: "Magical Realism",
        title: "Strange Beauty",
        fields: [
          f("magicalRealismDetail", "Beautiful Strange Detail", "longPrompt", "What detail felt impossible and perfect?"),
          f("magicalRealismOrdinaryMagic", "Ordinary Thing Made Magical", "longPrompt", "What everyday thing changed?"),
          f("magicalRealismEmotion", "Emotional Meaning", "longPrompt", "What feeling did the magic carry?"),
          f("magicalRealismKeepsake", "Magical Keepsake", "longPrompt", "What ordinary object became magical in your mind?"),
        ],
      },
    },
  ],

  adventure: [
    {
      id: "adventure-quest",
      left: {
        eyebrow: "Adventure Ledger",
        title: "The Journey",
        fields: [
          f("adventureDestination", "Favorite Destination", "longPrompt", "Where did the journey take you?"),
          f("adventureObstacle", "Biggest Obstacle", "longPrompt", "What stood in the way?"),
          f("adventureCompanion", "Travel Companion", "longPrompt", "Who would you want beside you?"),
          f("adventureDiscovery", "Best Discovery", "longPrompt", "What was uncovered?"),
        ],
      },
      right: {
        eyebrow: "Adventure Ledger",
        title: "Quest Notes",
        fields: [
          f("adventureDanger", "Danger Level", "longPrompt", "Low, exciting, terrifying, impossible..."),
          f("adventureSurvival", "Survival Moment", "longPrompt", "When did survival feel uncertain?"),
          f("adventureVisit", "Place You’d Visit", "longPrompt", "Where would you go?"),
          f("adventureQuestSatisfaction", "Quest Satisfaction", "longPrompt", "Did the journey pay off?"),
          f("adventureSouvenir", "Souvenir From The Journey", "longPrompt", "What would you bring back from the adventure?"),
        ],
      },
    },
  ],

  classics: [
    {
      id: "classics-ledger",
      left: {
        eyebrow: "Classics Ledger",
        title: "Then & Now",
        fields: [
          f("classicsAgedWell", "What Aged Well", "longPrompt", "What still works today?"),
          f("classicsAgedPoorly", "What Aged Poorly", "longPrompt", "What did not age well?"),
          f("classicsStillMatters", "Why It Still Matters", "longPrompt", "Why does this book still get read?"),
          f("classicsRelevantQuote", "Quote Still Relevant", "longPrompt", "What line still speaks today?"),
        ],
      },
      right: {
        eyebrow: "Classics Ledger",
        title: "Modern Reading",
        fields: [
          f("classicsSurpriseCharacter", "Character Who Surprised You", "longPrompt", "Who felt different than expected?"),
          f("classicsLivingTheme", "Theme Still Alive Today", "longPrompt", "What theme still exists now?"),
          f("classicsModernConnection", "Modern Connection", "longPrompt", "What did this remind you of today?"),
          f("classicsKeepsake", "Object That Still Feels Timeless", "longPrompt", "What object, setting, or image still works today?"),
        ],
      },
    },
  ],

  youngAdult: [
  {
    id: "young-adult-coming-of-age",
    left: {
      eyebrow: "Young Adult Ledger",
      title: "Coming Of Age",
      fields: [
        f("yaComingOfAgeMoment", "Coming-Of-Age Moment", "longPrompt", "What moment showed the character growing up, changing, or finding themselves?"),
        f("yaIdentityTheme", "Identity Theme", "longPrompt", "What did this book explore about identity, belonging, confidence, or self-discovery?"),
        f("yaFriendship", "Friendship Notes", "longPrompt", "Which friendship mattered most, and why?"),
        f("yaFamilyDynamic", "Family Dynamic", "longPrompt", "How did family shape the story?"),
        f("yaBigChoice", "Big Choice", "longPrompt", "What decision changed the character’s path?"),
      ],
    },
    right: {
      eyebrow: "Young Adult Ledger",
      title: "Growing Into The Story",
      fields: [
        f("yaRelatableMoment", "Most Relatable Moment", "longPrompt", "What felt especially real, familiar, or honest?"),
        f("yaLessonLearned", "Lesson Learned", "longPrompt", "What did the character learn by the end?"),
        f("yaPressure", "Pressure They Faced", "longPrompt", "School, family, power, romance, friendship, survival, expectations..."),
        f("yaVoice", "Narrator / Voice Notes", "longPrompt", "Did the voice feel believable, emotional, funny, intense, or honest?"),
        f("yaYoungerSelf", "Would Younger You Have Loved This?", "longPrompt", "Would this book have mattered to you as a teen? Why or why not?"),
      ],
    },
  },
],

  urbanFantasy: [
    {
      id: "urban-fantasy",
      left: {
        eyebrow: "Urban Fantasy",
        title: "Magic In The City",
        fields: [
          f("urbanFantasySociety", "Hidden Magical Society", "longPrompt", "How does magic exist beside normal life?"),
          f("urbanFantasyCity", "City Setting", "longPrompt", "How did the city shape the story?"),
          f("urbanFantasyPolitics", "Supernatural Politics", "longPrompt", "Who has power?"),
          f("urbanFantasyCreature", "Favorite Creature In Modern World", "longPrompt", "Who fit the modern setting best?"),
        ],
      },
      right: {
        eyebrow: "Urban Fantasy",
        title: "Secret Rules",
        fields: [
          f("urbanFantasyOrdinaryMagic", "Ordinary Meets Magic Moment", "longPrompt", "When did normal life collide with magic?"),
          f("urbanFantasyRules", "Secret World Rules", "longPrompt", "What rules govern the hidden world?"),
          f("urbanFantasyCityObject", "Magical City Object", "longPrompt", "What ordinary city item felt magical?"),
          f("urbanFantasyHiddenDoor", "Hidden Doorway", "longPrompt", "Where did the normal world open into something else?"),
        ],
      },
    },
  ],

    darkAcademia: [
    {
      id: "dark-academia",
      left: {
        eyebrow: "Dark Academia",
        title: "Obsessions & Secrets",
        fields: [
          f("darkAcademiaSetting", "Academic Setting", "longPrompt", "School, university, archive, society..."),
          f("darkAcademiaSociety", "Secret Society", "longPrompt", "What hidden group fascinated you?"),
          f("darkAcademiaObsession", "Central Obsession", "longPrompt", "Knowledge, power, art, status, revenge..."),
          f("darkAcademiaGrayChoice", "Morally Gray Choice", "longPrompt", "What choice crossed a line?"),
          f("darkAcademiaAtmosphere", "Best Atmosphere", "longPrompt", "What made the setting feel intoxicating?"),
        ],
      },
      right: {
        eyebrow: "Dark Academia",
        title: "Study Notes",
        fields: [
          f("darkAcademiaLibraryScene", "Favorite Library / Classroom Scene", "longPrompt", "Where did the atmosphere peak?"),
          f("darkAcademiaRivalry", "Intellectual Rivalry", "longPrompt", "Who challenged who?"),
          f("darkAcademiaStudyThere", "Would You Study There?", "longPrompt", "Would you survive this environment?"),
          f("darkAcademiaObject", "Object On The Desk", "longPrompt", "What item perfectly represents this story?"),
          f("darkAcademiaQuote", "Line Worth Underlining", "longPrompt", "What line belongs in the margins?"),
        ],
      },
    },
  ],

  cozyFantasy: [
    {
      id: "cozy-fantasy",
      left: {
        eyebrow: "Cozy Fantasy",
        title: "Comfort Magic",
        fields: [
          f("cozyFantasyComfort", "Comfort Rating", "longPrompt", "How comforting was the experience?"),
          f("cozyFantasyLocation", "Favorite Cozy Location", "longPrompt", "Inn, bakery, cottage, tea shop..."),
          f("cozyFantasyFoundFamily", "Found Family", "longPrompt", "Who felt like home?"),
          f("cozyFantasyGentleMoment", "Favorite Gentle Moment", "longPrompt", "What scene felt warm and safe?"),
          f("cozyFantasyFood", "Food / Drink Notes", "longPrompt", "What made you hungry?"),
        ],
      },
      right: {
        eyebrow: "Cozy Fantasy",
        title: "Warm Evenings",
        fields: [
          f("cozyFantasyLowStakesMagic", "Low-Stakes Magic", "longPrompt", "What magical detail made you smile?"),
          f("cozyFantasyRainyDay", "Character You’d Spend A Rainy Day With", "longPrompt", "Who would you invite over?"),
          f("cozyFantasyScent", "What Would This Place Smell Like?", "longPrompt", "Tea, bread, books, rain, flowers..."),
          f("cozyFantasyDrink", "Signature Drink", "longPrompt", "What would the house specialty be?"),
          f("cozyFantasyKeepsake", "Tiny Keepsake", "longPrompt", "What small object belongs with this story?"),
        ],
      },
    },
  ],

  mythology: [
    {
      id: "mythology-retelling",
      left: {
        eyebrow: "Mythology Retelling",
        title: "Old Stories",
        fields: [
          f("mythologyOriginal", "Original Myth", "longPrompt", "Which myth inspired the story?"),
          f("mythologyReinterpretation", "Favorite Reinterpretation", "longPrompt", "What fresh twist worked best?"),
          f("mythologyFigure", "Favorite Mythological Figure", "longPrompt", "Who stole the story?"),
          f("mythologyCreativeChange", "Creative Change", "longPrompt", "What change surprised you?"),
        ],
      },
      right: {
        eyebrow: "Mythology Retelling",
        title: "Retold Legends",
        fields: [
          f("mythologyStayedTrue", "What Stayed True", "longPrompt", "What honored the source material?"),
          f("mythologyImproved", "What Changed For The Better", "longPrompt", "What update worked?"),
          f("mythologyNextRetelling", "Myth You Want Retold Next", "longPrompt", "What story deserves a retelling?"),
          f("mythologySymbol", "Symbol Of The Myth", "longPrompt", "What object captures the story?"),
          f("mythologyDivineMoment", "Most Divine Moment", "longPrompt", "When did the myth feel biggest?"),
        ],
      },
    },
  ],

  fairyTale: [
    {
      id: "fairy-tale-retelling",
      left: {
        eyebrow: "Fairy Tale Retelling",
        title: "Once Upon A Time",
        fields: [
          f("fairyTaleOriginal", "Original Tale", "longPrompt", "Which fairy tale inspired this story?"),
          f("fairyTaleTwist", "Favorite Twist", "longPrompt", "What changed the story in the best way?"),
          f("fairyTaleVillain", "Villain Reinterpretation", "longPrompt", "Did the villain become more interesting?"),
          f("fairyTaleObject", "Best Magical Object", "longPrompt", "What item felt iconic?"),
        ],
      },
      right: {
        eyebrow: "Fairy Tale Retelling",
        title: "Happily Ever After?",
        fields: [
          f("fairyTaleDarkestChange", "Darkest Change", "longPrompt", "What became darker than the original?"),
          f("fairyTaleNostalgia", "Most Nostalgic Moment", "longPrompt", "What reminded you of the original story?"),
          f("fairyTaleHonor", "Did It Honor The Original?", "longPrompt", "Did it feel respectful to the source?"),
          f("fairyTaleEnding", "Favorite Retold Ending", "longPrompt", "How did this version end differently?"),
          f("fairyTaleKeepsake", "Fairy Tale Keepsake", "longPrompt", "What object belongs in the storybook?"),
        ],
      },
    },
  ],

  womensFiction: [
    {
      id: "womens-fiction",
      left: {
        eyebrow: "Women's Fiction",
        title: "Life Changes",
        fields: [
          f("womensChallenge", "Life Challenge", "longPrompt", "What challenge defined the story?"),
          f("womensGrowth", "Personal Growth", "longPrompt", "How did the character change?"),
          f("womensRelationship", "Friendship / Family Dynamic", "longPrompt", "What relationship mattered most?"),
          f("womensChoice", "Most Relatable Choice", "longPrompt", "What decision felt real?"),
        ],
      },
      right: {
        eyebrow: "Women's Fiction",
        title: "Looking Forward",
        fields: [
          f("womensTurningPoint", "Emotional Turning Point", "longPrompt", "When did everything shift?"),
          f("womensLesson", "Lesson Learned", "longPrompt", "What takeaway stayed with you?"),
          f("womensFuture", "Character’s Future", "longPrompt", "What do you imagine happens next?"),
          f("womensKeepsake", "Object From Her Journey", "longPrompt", "What item symbolizes the story?"),
        ],
      },
    },
  ],

  christian: [
    {
      id: "christian-fiction",
      left: {
        eyebrow: "Faith-Based Fiction",
        title: "Faith Journey",
        fields: [
          f("christianJourney", "Faith Journey", "longPrompt", "How did faith shape the story?"),
          f("christianMessage", "Meaningful Message", "longPrompt", "What truth stood out?"),
          f("christianQuestion", "Spiritual Question", "longPrompt", "What question did the story explore?"),
          f("christianInspiring", "Inspiring Moment", "longPrompt", "What scene encouraged you?"),
        ],
      },
      right: {
        eyebrow: "Faith-Based Fiction",
        title: "Grace Notes",
        fields: [
          f("christianStruggle", "Struggle With Belief", "longPrompt", "What challenge tested faith?"),
          f("christianGrace", "Favorite Act Of Grace", "longPrompt", "What moment felt meaningful?"),
          f("christianTakeaway", "Biggest Takeaway", "longPrompt", "What stays with you now?"),
          f("christianSymbol", "Faith Symbol", "longPrompt", "What object best represents the message?"),
        ],
      },
    },
  ],

  manga: [
    {
      id: "manga-graphic-novel",
      left: {
        eyebrow: "Manga / Graphic Novel",
        title: "Visual Storytelling",
        fields: [
          f("mangaPanel", "Favorite Panel", "longPrompt", "Which page deserves a frame?"),
          f("mangaArtStyle", "Art Style Rating", "longPrompt", "What stood out visually?"),
          f("mangaStorytelling", "Visual Storytelling", "longPrompt", "How did the art tell the story?"),
          f("mangaCharacterDesign", "Favorite Character Design", "longPrompt", "Who looked the coolest?"),
        ],
      },
      right: {
        eyebrow: "Manga / Graphic Novel",
        title: "On The Page",
        fields: [
          f("mangaActionSpread", "Best Action Spread", "longPrompt", "What scene exploded off the page?"),
          f("mangaEmotionalPage", "Most Emotional Page", "longPrompt", "What panel hit hardest?"),
          f("mangaLinework", "Color / Linework Notes", "longPrompt", "What artistic choice stood out?"),
          f("mangaPoster", "Panel You’d Turn Into A Poster", "longPrompt", "What would you hang on a wall?"),
        ],
      },
    },
  ],

  shortStories: [
    {
      id: "short-story-collection",
      left: {
        eyebrow: "Short Story Collection",
        title: "Collection Notes",
        fields: [
          f("shortStoryFavorite", "Favorite Story", "longPrompt", "Which story won?"),
          f("shortStoryWeakest", "Weakest Story", "longPrompt", "Which story missed the mark?"),
          f("shortStoryNovel", "Story You’d Expand Into A Novel", "longPrompt", "Which one deserves more pages?"),
          f("shortStoryTheme", "Connecting Theme", "longPrompt", "What tied everything together?"),
        ],
      },
      right: {
        eyebrow: "Short Story Collection",
        title: "Final Thoughts",
        fields: [
          f("shortStoryEnding", "Most Surprising Ending", "longPrompt", "Which ending shocked you most?"),
          f("shortStoryStayed", "Story That Stayed Longest", "longPrompt", "Which one still lingers?"),
          f("shortStoryObject", "Object From The Collection", "longPrompt", "What item represents the collection?"),
          f("shortStoryMood", "Overall Mood", "longPrompt", "What feeling did the collection leave behind?"),
        ],
      },
    },
  ],

  smut: [
    {
      id: "smut-romance",
      left: {
        eyebrow: "Spicy Romance Ledger",
        title: "Heat & Chemistry",
        fields: [
          f("smutFavoriteScene", "Favorite Spicy Scene", "longPrompt", "What scene worked best?"),
          f("smutConsent", "Consent / Communication Notes", "longPrompt", "How was communication handled?"),
          f("smutChemistry", "Chemistry Rating Notes", "longPrompt", "Did the chemistry feel believable?"),
          f("smutEmotionalConnection", "Emotional Connection", "longPrompt", "Did the feelings match the attraction?"),
        ],
      },
      right: {
        eyebrow: "Spicy Romance Ledger",
        title: "Balance",
        fields: [
          f("smutTropes", "Kink / Trope Notes", "longPrompt", "What tropes were present?"),
          f("smutBalance", "Plot-To-Spice Balance", "longPrompt", "Too much, too little, just right?"),
          f("smutWarnings", "Would Recommend With Warnings?", "longPrompt", "What should readers know beforehand?"),
          f("smutMemorableMoment", "Most Memorable Romantic Moment", "longPrompt", "What stayed with you after finishing?"),
        ],
      },
    },
  ],
};

export const endingPages: LedgerPage[] = [
  {
    id: "book-weather",
    left: {
      eyebrow: "Book Weather",
      title: "Atmosphere",
      fields: [
        f("bookSeason", "Season", "input", "Autumn, winter, spring rain, summer heat..."),
        f("bookWeather", "Weather", "input", "Thunderstorm, fog, soft snow, humid night..."),
        f("bookTimeOfDay", "Time Of Day", "input", "Midnight, dawn, golden hour, blue morning..."),
        f("bookLighting", "Lighting", "input", "Candlelight, moonlight, neon glow, gray daylight..."),
        f("bookColorPalette", "Color Palette", "input", "Forest green, candle gold, ink black, blood red..."),
        f("bookSound", "Sound", "input", "Rain on windows, crackling fire, ocean waves, footsteps..."),
        f("bookTexture", "Texture", "input", "Velvet, cracked leather, cold glass, soft wool..."),
      ],
    },
    right: {
      eyebrow: "Book Weather",
      title: "Senses",
      fields: [
        f("bookScent", "Scent", "input", "Old paper, rain, smoke, roses, pine, vanilla..."),
        f("bookTaste", "Taste", "input", "Cinnamon, black coffee, honey, salt air, peppermint..."),
        f("bookDrink", "Drink", "input", "Tea, coffee, cider, cocoa, lemonade, whiskey..."),
        f("bookCup", "Cup Or Glass", "input", "Teacup, mug, goblet, flask, tumbler, shot glass..."),
        f("bookSnack", "Snack Or Treat", "input", "Cookies, berries, chocolate, popcorn, bread, candy..."),
        f("bookPlace", "Place", "input", "A castle, coffee shop, rainy bedroom, old library..."),
        f("bookFeeling", "If This Book Were A Feeling...", "longPrompt", "Describe the emotional weather of the story..."),
      ],
    },
  },
  {
    id: "little-keepsakes",
    left: {
      eyebrow: "Reader Scrapbook",
      title: "Little Keepsakes",
      fields: [
        f("keepsakeBookmark", "Bookmark Style", "input", "Pressed flowers, ribbon, metal charm, handwritten note..."),
        f("keepsakeObject", "Object That Represents It", "input", "Key, locket, dagger, letter, map, crown, ribbon..."),
        f("keepsakeTrinket", "Tiny Trinket", "input", "Coin, charm, compass, feather, vial, pin..."),
        f("keepsakeWearable", "Wearable Item", "input", "Ring, scarf, gloves, necklace, hair ribbon, cloak pin..."),
        f("keepsakePaperItem", "Paper Item", "input", "Map, letter, prophecy, ticket, diary page, wanted poster..."),
        f("keepsakeScentItem", "Scented Item", "input", "Candle, bath bomb, lotion, incense, oil, soap..."),
      ],
    },
    right: {
      eyebrow: "Reader Scrapbook",
      title: "Story Relics",
      fields: [
        f("mainCharacterCarry", "What Would The Main Character Always Carry?", "input", "A knife, book, ribbon, coin, flower, letter, charm..."),
        f("storySymbol", "Symbol Of The Story", "input", "Rose, raven, sword, teacup, moon, crown, flame..."),
        f("characterPocket", "What Would Be In A Character’s Pocket?", "input", "A note, lucky coin, flower petal, ticket, key..."),
        f("overlookedDetail", "Tiny Detail That Might Matter", "input", "An object, phrase, color, place, gift, or repeated image..."),
        f("inUniverseNewspaper", "In-World Headline", "input", "What would the newspaper headline be inside this story?"),
        f("bookstoreSection", "Bookstore Section It Belongs In", "input", "Dark romance, cozy magic, haunted houses, soft heartbreak..."),
      ],
    },
  },
  {
    id: "story-legacy",
    left: {
      eyebrow: "Story Legacy",
      title: "After Closing The Book",
      fields: [
        f("initialRating", "Initial Rating", "rating", "", { ratingIcon: "overall" }),
        f("rereadRating", "Rereadability Rating", "rating", "", { ratingIcon: "reread" }),
        f("wouldReread", "Would You Reread?", "input", "Yes, no, maybe, only in a certain mood..."),
        f("wouldRecommend", "Would You Recommend It?", "dropdown", "", { options: recommendOptions }),
        f("recommendToWhom", "Who Would You Recommend It To?", "longPrompt", "A friend, younger self, fantasy reader, someone healing...", {
          showIf: (book) =>
            book.wouldRecommend === "YES" ||
            book.wouldRecommend === "WITH_WARNINGS" ||
            book.wouldRecommend === "CERTAIN_READERS",
        }),
        f("readerWhoNeedsThis", "Reader Who Needs This Book", "longPrompt", "What kind of reader would find this at the perfect time?"),
        f("bookAftertaste", "Book Aftertaste", "input", "Sweet, bitter, haunting, hopeful, cozy, electric..."),
      ],
    },
    right: {
      eyebrow: "Lasting Memory",
      title: "What Remains",
      fields: [
        f("whatStayedWithYou", "What Stayed With You?", "longPrompt", "A character, feeling, lesson, scene..."),
        f("changedPerspective", "Did It Change Your Perspective?", "longPrompt", "Did this story shift how you see anything?"),
        f("lessonThatRemains", "Lesson That Remains", "longPrompt", "What did the story leave behind?"),
        f("oneSentenceMemory", "One Sentence Memory", "input", "If you remembered this book in one sentence..."),
        f("detailYouStillPicture", "Detail You Still Picture", "longPrompt", "What image, object, place, or moment still appears in your mind?"),
      ],
    },
  },
  {
    id: "why-this-story-found-me",
    left: {
      eyebrow: "Reader Connection",
      title: "Why This Story Found Me",
      fields: [
        f("whyItMatters", "Why This Story Matters", "longPrompt", "After reading, why did this story matter to you?"),
        f("lifeSeason", "What Was Happening In Life?", "longPrompt", "What season of life were you in when you read it?"),
        f("readerNeed", "What Did This Story Give You?", "longPrompt", "Comfort, escape, learning, healing, fun, perspective..."),
        f("whyNow", "Why Did This Book Fit This Moment?", "longPrompt", "Why did this story belong in this part of your life?"),
      ],
    },
    right: {
      eyebrow: "This Book In My Life",
      title: "The Moment",
      fields: [
        f("bookAssociation", "What Will You Associate With It?", "longPrompt", "A place, person, season, trip, room, or version of yourself..."),
        f("comfortEscapeChallenge", "What Kind Of Read Was This?", "input", "Comfort read, escape read, challenge read, hype read..."),
        f("readingMemorySnapshot", "Reading Memory Snapshot", "longPrompt", "What memory comes back when you think about reading it?"),
        f("versionOfMe", "The Version Of Me Who Read This", "longPrompt", "Who were you when this book found you?"),
      ],
    },
  },
  {
    id: "final-verdict",
    left: {
      eyebrow: "Final Verdict",
      title: "After Sitting With It",
      fields: [
        f("finalRating", "Final Overall Rating", "rating", "", { ratingIcon: "overall" }),
        f("finalThoughts", "Final Thoughts", "longPrompt", "After thinking about it, what do you really feel about this book?"),
        f("favoriteThing", "Favorite Thing", "longPrompt", "What did the book do best?"),
        f("leastFavoriteThing", "Least Favorite Thing", "longPrompt", "What did not work for you?"),
        f("wouldChangeAnything", "Would You Change Anything?", "longPrompt", "If you could alter one thing, what would it be?"),
      ],
    },
    right: {
      eyebrow: "Final Verdict",
      title: "Loose Threads",
      fields: [
        f("plotHoles", "Did Anything Not Make Sense?", "longPrompt", "Were there any plot holes, confusing choices, or unanswered details?"),
        f("breadcrumbsOrPlotHoles", "Plot Hole Or Future Setup?", "longPrompt", "Could anything unresolved be a breadcrumb, Easter egg, foreshadowing, sequel setup, or was it simply a plot hole?", {
          showIf: isSeries,
        }),
        f("foreshadowingInHindsight", "Best Foreshadowing In Hindsight", "longPrompt", "Looking back, what clue, warning, or detail feels obvious now that you know the ending?"),
        f("payoffMoment", "Favorite Payoff Moment", "longPrompt", "What reveal, callback, setup, or resolution paid off best?"),
        f("questionStillHaunts", "What Question Still Haunts You?", "longPrompt", "What are you still wondering about?"),
        f("wouldContinueSeries", "Would You Continue The Series?", "input", "Yes, no, maybe, already obsessed...", { showIf: isSeries }),
        f("wouldReadAuthorAgain", "Would You Read This Author Again?", "input", "Absolutely, maybe, not sure, no..."),
      ],
    },
  },
  {
    id: "reader-awards",
    left: {
      eyebrow: "Reader Awards",
      title: "This Book Wins",
      fields: [
        f("awardBestCharacter", "Best Character", "longPrompt", "Who takes the crown, and why?"),
        f("awardBestVillain", "Best Villain", "longPrompt", "Who was unforgettable?"),
        f("awardBestCouple", "Best Couple / Duo", "longPrompt", "Romantic, friendship, rivals, siblings..."),
        f("awardBestPlotTwist", "Best Plot Twist", "longPrompt", "What twist deserves an award?"),
        f("awardBestWorldbuilding", "Best Worldbuilding", "longPrompt", "What detail made the world real?"),
        f("awardBestFriendship", "Best Friendship", "longPrompt", "Who had the best bond?"),
      ],
    },
    right: {
      eyebrow: "Reader Awards",
      title: "Special Honors",
      fields: [
        f("awardMostEmotional", "Most Emotional", "longPrompt", "What wrecked you?"),
        f("awardMostComforting", "Most Comforting", "longPrompt", "What felt like a warm blanket?"),
        f("awardMostUnhinged", "Most Unhinged", "longPrompt", "What made you say WHAT did I just read?"),
        f("awardBeautifulWriting", "Most Beautiful Writing", "longPrompt", "Gorgeous prose, chapter, or quote..."),
        f("awardCinematicMoment", "Most Cinematic Moment", "longPrompt", "What scene played like a movie?"),
        f("awardNeedsTherapy", "Character Who Needs Therapy", "longPrompt", "Who needs help immediately?"),
      ],
    },
  },
  {
    id: "fan-casting",
    left: {
      eyebrow: "Dream Adaptation",
      title: "Fan Casting",
      fields: [
        f("fanCastingBoard", "Open Fan Casting Board Later", "input", "Coming soon: full character casting board"),
        f("castMainCharacter", "Main Character", "input", "Who would play them?"),
        f("castLoveInterest", "Love Interest", "input", "Optional"),
        f("castVillain", "Villain", "input", "Who would make them unforgettable?"),
        f("castSideCharacter", "Side Character", "input", "Who steals the show?"),
      ],
    },
    right: {
      eyebrow: "Dream Adaptation",
      title: "On Screen",
      fields: [
        f("dreamDirector", "Dream Director / Style", "input", "HBO, A24, Netflix, cinematic, animated..."),
        f("dreamSceneAdapted", "Scene I Need Adapted", "longPrompt", "What scene must make it to the screen?"),
        f("adaptationStyle", "Adaptation Style", "input", "Movie, limited series, animated, audiobook cast..."),
        f("adaptationNotes", "Adaptation Notes", "longPrompt", "What would make the adaptation work?"),
      ],
    },
  },
  {
    id: "music-atmosphere",
    left: {
      eyebrow: "Music & Atmosphere",
      title: "Reading Soundtrack",
      fields: [
        f("songTitle", "Song", "input", "e.g. The Night We Met"),
        f("songArtist", "Artist", "input", "e.g. Lord Huron"),
        f("playlistLink", "Playlist Link", "input", "Spotify, YouTube, Apple Music..."),
        f("readingSetting", "Reading Setting", "input", "Bedroom, porch, library, car, coffee shop..."),
      ],
    },
    right: {
      eyebrow: "Atmosphere",
      title: "The Vibe",
      fields: [
        f("atmosphere", "Atmosphere", "dropdown", "", { options: atmosphereOptions }),
        f("musicMood", "Music Mood", "input", "Soft, epic, tragic, romantic, eerie..."),
        f("soundtrackMoment", "Scene This Song Belongs To", "longPrompt", "Where would the soundtrack swell?"),
        f("readingVibeNotes", "Reading Vibe Notes", "longPrompt", "What did this book feel like while reading?"),
      ],
    },
  },
  {
    id: "reading-report-card",
    left: {
      eyebrow: "Reading Report Card",
      title: "Final Ratings",
      fields: [
        f("excitementRating", "Excitement Rating", "rating", "", { ratingIcon: "excitement" }),
        f("currentExcitementRating", "Excitement While Reading", "rating", "", { ratingIcon: "excitement" }),
        f("predictedRating", "Predicted Rating", "rating", "", { ratingIcon: "overall" }),
        f("currentRating", "Current Rating", "rating", "", { ratingIcon: "overall" }),
        f("initialRating", "Initial Rating", "rating", "", { ratingIcon: "overall" }),
      ],
    },
    right: {
      eyebrow: "",
      title: "",
      fields: [
        f("finalRating", "Final Overall Rating", "rating", "", { ratingIcon: "overall" }),
        f("rereadRating", "Rereadability", "rating", "", { ratingIcon: "reread" }),
        f("emotionalDevastationRating", "Emotional Devastation", "rating", "", { ratingIcon: "devastated" }),
        f("romanceRating", "Romance", "rating", "", { ratingIcon: "romance", showIf: hasRomance }),
        f("spiceRating", "Spice", "rating", "", { ratingIcon: "spice", showIf: hasRomance }),
        f("horrorRating", "Scare Factor", "rating", "", { ratingIcon: "horror", showIf: hasScareGenre }),
      ],
    },
  },
  {
    id: "loose-pages",
    left: {
      eyebrow: "Final Page",
      title: "Final Scribbles",
      fields: [
        f("scrapbookThoughts", "Anything Else?", "longPrompt", "Anything that did not belong anywhere else. Last thoughts, strange observations, theories, memories, complaints, confessions, or anything your reading self wants to leave behind."),
      ],
    },
    right: {
      eyebrow: "",
      title: "",
      fields: [],
    },
  },
];

export const extraGenreAliases: Record<string, string[]> = {
  romantasy: ["romantasy", "fantasy", "romance"],
  romancy: ["romantasy", "fantasy", "romance"],
  romanticy: ["romantasy", "fantasy", "romance"],
  romancey: ["romantasy", "fantasy", "romance"],

  fantasy: ["fantasy"],
  romance: ["romance"],
  "paranormal romance": ["paranormal", "romance"],
  paranormal: ["paranormal"],
  mystery: ["mystery"],
  "cozy mystery": ["cozyMystery"],
  thriller: ["thriller"],
  horror: ["horror"],
  gothic: ["gothic"],
  "sci-fi": ["scifi"],
  "science fiction": ["scifi"],
  scifi: ["scifi"],
  dystopian: ["dystopian"],
  "historical fiction": ["historical"],
  "historical romance": ["historicalRomance", "romance"],
  nonfiction: ["nonfiction"],
  memoir: ["memoir"],
  biography: ["memoir"],
  "memoir/biography": ["memoir"],
  "self help": ["selfHelp"],
  "self-help": ["selfHelp"],
  "true crime": ["trueCrime"],
  poetry: ["poetry"],
  contemporary: ["contemporary"],
  literary: ["contemporary"],
  "contemporary/literary": ["contemporary"],
  "magical realism": ["magicalRealism"],
  adventure: ["adventure"],
  classics: ["classics"],
  classic: ["classics"],
  "urban fantasy": ["urbanFantasy", "fantasy"],
  "dark academia": ["darkAcademia"],
  "cozy fantasy": ["cozyFantasy", "fantasy"],
  mythology: ["mythology"],
  "mythology retelling": ["mythology"],
  "fairy tale": ["fairyTale"],
  "fairy tale retelling": ["fairyTale"],
  "women's fiction": ["womensFiction"],
  "womens fiction": ["womensFiction"],
  christian: ["christian"],
  "faith-based": ["christian"],
  "faith based": ["christian"],
  "christian fiction": ["christian"],
  manga: ["manga"],
  "graphic novel": ["manga"],
  "manga/graphic novel": ["manga"],
  "short story": ["shortStories"],
  "short story collection": ["shortStories"],
  smut: ["smut", "romance"],
  erotic: ["smut", "romance"],
  "erotic romance": ["smut", "romance"],
  "young adult": ["youngAdult"],
ya: ["youngAdult"],
"ya fiction": ["youngAdult"],
"young adult fiction": ["youngAdult"],
"young adult fantasy": ["youngAdult", "fantasy"],
"ya fantasy": ["youngAdult", "fantasy"],
"young adult romance": ["youngAdult", "romance"],
"ya romance": ["youngAdult", "romance"],
"young adult urban fantasy": ["youngAdult", "urbanFantasy", "fantasy"],
"ya urban fantasy": ["youngAdult", "urbanFantasy", "fantasy"],
"young adult dystopian": ["youngAdult", "dystopian"],
"ya dystopian": ["youngAdult", "dystopian"],
};

export function getGenrePages(book: NewBook): LedgerPage[] {
  const genre = normalizeText(book.genre || "");
  const keys = new Set<string>();

  Object.entries(extraGenreAliases).forEach(([term, moduleKeys]) => {
    if (genre.includes(normalizeText(term))) {
      moduleKeys.forEach((moduleKey) => keys.add(moduleKey));
    }
  });

  return Array.from(keys).flatMap((key) => genrePages[key] ?? []);
}

export function getLedgerPages(book: NewBook): LedgerPage[] {
  return [...basePages, ...getGenrePages(book), ...endingPages];
}