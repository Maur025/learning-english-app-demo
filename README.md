# Offline English Learning App

> **Working title:** Offline English Trainer  
> **Platform:** Android  
> **Primary language:** Kotlin  
> **UI:** Jetpack Compose  
> **Core principle:** Offline-first  
> **Primary learning approach:** Lexical Approach + Active Recall + Spaced Repetition  
> **Primary user:** Mauro  
> **Status:** Product and technical specification for initial implementation

---

## 1. Purpose

This project is a personal Android application for improving English through short, consistent daily sessions of approximately **30–60 minutes**.

The application is not intended to be a general-purpose language-learning platform in its first versions. It is designed around one concrete goal:

> **Convert useful English expressions from unknown or passive vocabulary into language that can be recognized, recalled, and eventually produced naturally.**

The first version focuses primarily on **vocabulary acquisition in context**, especially through phrases, collocations, lexical chunks, phrasal verbs, sentence patterns, and reusable expressions.

The application must be fully useful without an internet connection.

Future versions may add listening, pronunciation, speaking, speech recognition, and an optional on-device language model, but those capabilities must not be prerequisites for the core learning experience.

---

# 2. Product philosophy

The application must not optimize for:

- number of isolated words seen;
- gamification without learning value;
- streaks as the main success metric;
- memorizing dictionary translations;
- cloud dependency;
- AI-generated content for its own sake;
- maximizing session length.

It must optimize for:

- useful expressions learned in context;
- retention over time;
- active recall;
- productive vocabulary;
- consistent daily practice;
- progressively harder retrieval;
- personalized review;
- measurable improvement;
- low friction;
- offline availability.

The central product question is:

> **Can the user use today an expression that yesterday they could only recognize or did not know?**

---

# 3. Learning model

## 3.1 Lexical Approach

The application follows the idea that language is not learned only as:

```text
grammar + isolated words
```

Instead, a large part of practical language ability comes from reusable lexical units such as:

```text
figure something out
it turns out that...
as far as I know...
take a different approach
run into an issue
it depends on...
I'm trying to...
I'm not used to...
```

These units will be called **Expressions** throughout the application.

An Expression can represent:

- a lexical chunk;
- a collocation;
- a phrasal verb;
- a reusable sentence frame;
- an idiomatic expression;
- a prepositional pattern;
- a frequently used conversational construction;
- a technical-English construction.

An Expression is more important to the domain than an isolated `Word`.

---

## 3.2 Learning progression

Every expression should conceptually move through this progression:

```text
NEW
  ↓
SEEN
  ↓
RECOGNIZED
  ↓
RECALLABLE
  ↓
PRODUCTIVE
  ↓
MASTERED
```

These states describe increasing command of an expression.

### NEW

The user has not studied the expression.

### SEEN

The user has been introduced to its meaning and examples.

### RECOGNIZED

The user can identify or understand the expression when seeing it.

### RECALLABLE

The user can recover the expression with partial cues.

### PRODUCTIVE

The user can produce the expression from meaning/context without being shown it.

### MASTERED

The user has repeatedly demonstrated successful recall over sufficiently spaced intervals.

`MASTERED` does not mean that the expression will never be reviewed again. Reviews should become increasingly sparse.

---

## 3.3 Passive vs productive vocabulary

The application must distinguish at minimum between:

```text
Recognition
Production
```

Example:

```text
Expression: figure out

Recognition score: 95%
Production score: 62%
```

This distinction is fundamental.

A user understanding:

```text
We're trying to figure out what happened.
```

does not necessarily mean they can independently produce:

```text
I'm trying to figure out why it failed.
```

The application should therefore avoid a single generic "learned" counter.

---

# 4. Learning techniques

The application should combine multiple compatible techniques.

## 4.1 Sentence mining

Expressions are learned from realistic sentences rather than isolated dictionary entries.

Each expression should have one or more natural examples.

Example:

```text
Expression:
figure out

Meaning:
averiguar, descubrir o resolver algo

Pattern:
figure + object + out

Examples:
- I can't figure it out.
- We need to figure out what happened.
- I'm trying to figure out why it failed.
- Have you figured out how it works?
```

---

## 4.2 Active recall

The user must regularly attempt to reconstruct information before seeing the answer.

Bad review:

```text
figure out
→ averiguar
```

Better review:

```text
Estoy intentando averiguar por qué falla.

Write the sentence in English:
[                                    ]
```

Expected:

```text
I'm trying to figure out why it's failing.
```

The effort of retrieving the phrase is part of the learning process.

---

## 4.3 Spaced repetition

Reviews must be scheduled over increasingly longer intervals.

The scheduling algorithm must be encapsulated behind an interface so it can evolve independently from the UI and persistence model.

Initial candidates:

1. a simple deterministic scheduler;
2. SM-2-inspired scheduling;
3. FSRS in a later iteration.

Do not couple domain entities to a specific spaced-repetition algorithm.

Conceptual contract:

```kotlin
interface ReviewScheduler {
    fun schedule(
        state: LearningState,
        result: ReviewResult,
    ): LearningState
}
```

---

## 4.4 Pattern expansion

After learning a construction, the user should see or produce variations.

Example source pattern:

```text
I'm trying to figure out...
```

Possible variations:

```text
I'm trying to figure out what happened.
I'm trying to figure out how it works.
I'm trying to figure out why it's failing.
I'm trying to figure out whether this is possible.
```

This is intended to move the user from memorizing one sentence to understanding a reusable construction.

---

## 4.5 Difficulty progression

Exercises should gradually require less assistance.

Example progression:

```text
Recognition
    ↓
Multiple choice / meaning recognition
    ↓
Cloze
    ↓
Guided recall
    ↓
Translation / contextual recall
    ↓
Free production
```

A phrase should not become `PRODUCTIVE` merely because the user repeatedly succeeds at recognition exercises.

---

# 5. Primary product goal

The application should make it possible to open it every day and immediately know what to do.

The user should not need to manually decide:

- what to study;
- which expressions to review;
- what difficulty to use;
- whether to study new or old content;
- which weak areas need attention.

The learning engine should produce an appropriate daily session.

Conceptual example:

```text
Today's session

15 overdue reviews
5 new expressions
8 production exercises
5 recognition exercises
3 weak-expression exercises

Estimated duration: 42 min
```

---

# 6. Session duration

The target daily session is:

```text
30–60 minutes
```

The user should configure a preferred daily duration during onboarding.

Suggested options:

```text
15 min
30 min
45 min
60 min
```

The default for the initial user may be:

```text
45 min
```

The session generator should adapt the amount of work to this target instead of requiring completion of an arbitrary fixed number of exercises.

---

# 7. Offline-first requirement

Offline support is a **core product requirement**, not a fallback mode.

The following must work with airplane mode enabled:

- onboarding;
- learning new bundled expressions;
- reviews;
- active-recall exercises;
- progress tracking;
- statistics;
- expression library;
- search;
- settings;
- session generation;
- spaced repetition;
- content imported from local packs.

The application should be usable through this flow:

```text
Install APK
    ↓
Enable airplane mode
    ↓
Open application
    ↓
Complete onboarding
    ↓
Start learning
    ↓
Review
    ↓
Inspect progress
```

No account or cloud backend is required for V1.

---

# 8. Internet policy

Internet connectivity should be treated as an optional capability.

Future network-enabled features may include:

- downloading additional vocabulary packs;
- updating existing packs;
- downloading optional on-device AI models;
- backup;
- cross-device synchronization;
- importing external learning resources;
- retrieving audio packs;
- downloading pronunciation resources.

Loss of internet connectivity must not disable the core application.

Network state must therefore never be the source of truth for core learning data.

---

# 9. Scope of V1

## 9.1 Included

V1 must include:

- Kotlin Android application;
- Jetpack Compose UI;
- single-activity architecture;
- Compose Navigation;
- local persistence;
- Room/SQLite;
- DataStore;
- offline starter vocabulary;
- vocabulary packs;
- lexical expressions;
- expression examples;
- expression patterns;
- tags/categories;
- onboarding;
- daily learning goal;
- daily session generation;
- learning flow;
- review flow;
- multiple exercise types;
- active recall;
- spaced repetition;
- recognition tracking;
- production tracking;
- progress screen;
- expression library;
- local search;
- settings;
- database migrations;
- unit tests for core learning logic;
- persistence tests for important DAO behavior.

---

## 9.2 Explicitly excluded from V1

Do **not** implement these unless V1 is already functional:

- authentication;
- user accounts;
- backend server;
- cloud database;
- cloud synchronization;
- multiplayer/social features;
- leaderboards;
- online AI APIs;
- mandatory LLM;
- speech-to-text;
- text-to-speech learning flows;
- pronunciation scoring;
- listening exercises;
- speaking exercises;
- chat tutor;
- conversation role-play;
- complex achievements;
- ads;
- subscriptions;
- payments;
- Redis;
- custom cache layers;
- microservices;
- analytics requiring a server.

These features may be considered in later milestones.

---

# 10. Technology baseline

Use stable releases unless there is a concrete reason to use preview APIs.

Baseline checked on **2026-09-25**:

```text
Kotlin:              2.4.20 stable
Jetpack Compose:     1.12.1 stable
Material 3:          1.4.0 stable
Navigation:          2.10.2 stable
DataStore:           1.2.1 stable
Room 2.x:            2.8.5 stable
Room 3.x:            3.0.3 stable
```

Do not scatter versions throughout Gradle files.

Use:

```text
gradle/libs.versions.toml
```

as the single dependency-version catalog.

### Room choice

Prefer the stable Room line that provides the best compatibility with the selected Android/Kotlin toolchain at implementation time.

Room 3.x is stable as of the baseline date, but the project should not adopt it merely because its major version is newer.

The agent implementing the project should validate:

- Kotlin compatibility;
- KSP compatibility;
- Android Gradle Plugin compatibility;
- migration/testing support;
- required minimum SDK.

Record the chosen version in the version catalog.

---

# 11. Android architecture

Use a modern **single-activity Compose architecture**.

There should initially be exactly one Activity:

```text
MainActivity
```

`MainActivity` hosts the Compose application.

Navigation between application screens is performed through Compose Navigation.

Do not create:

```text
HomeActivity
LearnActivity
ReviewActivity
SettingsActivity
...
```

unless a future Android platform requirement creates a concrete need.

---

# 12. High-level architecture

Use clear architectural boundaries without implementing ceremonial or overly fragmented Clean Architecture.

```text
┌────────────────────────────────────────────┐
│                    UI                      │
│                                            │
│ Home / Learn / Practice / Library          │
│ Progress / Settings / Onboarding           │
│                                            │
│ Jetpack Compose                            │
└────────────────────┬───────────────────────┘
                     │
                 ViewModels
                     │
┌────────────────────▼───────────────────────┐
│                  Domain                    │
│                                            │
│ StartDailySession                          │
│ SubmitAnswer                               │
│ LearnExpression                            │
│ ReviewExpression                           │
│ GetProgress                                │
│                                            │
│ LearningEngine                             │
│ ReviewScheduler                            │
│ ExerciseGenerator                          │
│ AnswerEvaluator                            │
└────────────────────┬───────────────────────┘
                     │
                Repositories
                     │
┌────────────────────▼───────────────────────┐
│                   Data                     │
│                                            │
│ Room / SQLite                              │
│ DataStore                                  │
│ Bundled JSON dataset                       │
│ Dataset importer                           │
│                                            │
│ Future:                                    │
│ Pack downloader                            │
│ Sync                                       │
│ On-device AI                               │
└────────────────────────────────────────────┘
```

---

# 13. Architectural rules

Follow these rules unless there is a documented reason not to.

## UI

Composable functions:

- render state;
- emit UI events;
- contain no persistence logic;
- contain no DAO access;
- contain no scheduling algorithm;
- contain no repository implementation logic.

## ViewModels

ViewModels:

- coordinate screen state;
- invoke use cases/domain services;
- expose immutable UI state;
- handle user intents;
- avoid Android-context dependencies where possible.

## Domain

The domain layer contains:

- learning rules;
- exercise generation rules;
- progress calculations;
- spaced-repetition abstractions;
- learning-state transitions;
- session generation.

Domain logic should be unit-testable without Android instrumentation.

## Data

The data layer contains:

- Room entities;
- DAOs;
- repository implementations;
- DataStore implementation;
- seed-data import;
- database migrations;
- mapping between persistence models and domain models.

---

# 14. Dependency direction

Preferred direction:

```text
UI
 ↓
Domain
 ↓
Repository abstractions

Data
 ↓
implements repository abstractions
```

Do not allow the domain model to depend on Room annotations.

Do not pass Room entities directly to composables.

Use mappings where needed:

```text
Room Entity
   ↓
Domain Model
   ↓
UI Model / UI State
```

Avoid unnecessary mappings when they provide no boundary or semantic value.

---

# 15. Suggested module structure

Do not create dozens of modules before the project needs them.

A reasonable starting point is:

```text
app

core:model
core:domain
core:data
core:database

feature:onboarding
feature:home
feature:learn
feature:practice
feature:library
feature:progress
feature:settings
```

A simpler first commit is also acceptable:

```text
app
core
feature:learning
feature:progress
```

and then split modules as responsibilities become real.

The architecture should optimize for maintainability, not maximum module count.

---

# 16. Suggested package structure

Example:

```text
com.example.englishtrainer
│
├── app
│   ├── MainActivity.kt
│   ├── EnglishTrainerApp.kt
│   └── navigation/
│
├── core
│   ├── model/
│   ├── domain/
│   │   ├── learning/
│   │   ├── review/
│   │   ├── session/
│   │   └── progress/
│   ├── data/
│   │   ├── repository/
│   │   ├── mapper/
│   │   └── preferences/
│   └── database/
│       ├── entity/
│       ├── dao/
│       ├── relation/
│       ├── migration/
│       └── seed/
│
└── feature
    ├── onboarding/
    ├── home/
    ├── learn/
    ├── practice/
    ├── library/
    ├── progress/
    └── settings/
```

Adapt this structure if Gradle modules are introduced.

---

# 17. Persistence strategy

Use:

```text
Room
  ↓
SQLite
```

for structured learning data.

Use DataStore for small application/user preferences.

---

# 18. Room responsibilities

Room should store data such as:

- expressions;
- examples;
- patterns;
- tags;
- packs;
- reviews;
- learning state;
- sessions;
- exercises/results when useful;
- historical progress;
- mastery information.

Room is the local source of truth for learning state.

---

# 19. DataStore responsibilities

DataStore should store lightweight preferences such as:

```text
onboardingCompleted
dailyGoalMinutes
newExpressionsPerDay
preferredContentPackIds
theme
locale
exercisePreferences
```

Do not use DataStore as a relational database.

Do not put thousands of expressions into DataStore.

---

# 20. Cache policy

V1 does **not** require a custom cache layer.

Room is sufficiently fast for the expected amount of local content.

Normal in-memory UI/session state is acceptable:

```text
SessionUiState
CurrentExercise
RemainingExercises
TemporaryAnswer
SessionScore
```

but Room remains the persistent source of truth.

Do not introduce:

- Redis;
- disk cache separate from Room;
- custom LRU cache;
- duplicated persistent caches;

without profiling evidence.

---

# 21. Core domain model

The central domain object is `Expression`, not `Word`.

Conceptual model:

```kotlin
data class Expression(
    val id: ExpressionId,
    val phrase: String,
    val primaryMeaning: String,
    val explanation: String?,
    val difficulty: Difficulty,
    val level: CefrLevel?,
    val examples: List<Example>,
    val patterns: List<ExpressionPattern>,
    val tags: Set<Tag>,
)
```

Do not assume this exact class maps one-to-one to a Room table.

---

# 22. Expression

An expression represents a reusable lexical unit.

Examples:

```text
figure out
it turns out that...
run into an issue
take into account
as far as I know...
be used to + -ing
I'm trying to...
```

Suggested fields:

```text
id
phrase
primaryMeaning
explanation
difficulty
CEFR level (optional)
source
packId
createdAt
updatedAt
```

---

# 23. Example sentence

One expression may have multiple examples.

Suggested fields:

```text
id
expressionId
english
spanish
context
difficulty
isPrimary
```

Example:

```text
Expression:
figure out

Examples:
1. I can't figure it out.
2. We need to figure out what happened.
3. I'm trying to figure out why it failed.
4. Have you figured out how it works?
```

---

# 24. Expression pattern

A pattern explains reusable structure.

Example:

```text
expression:
figure out

pattern:
figure + object + out
```

Another:

```text
expression:
be used to

pattern:
be used to + noun / -ing
```

Suggested fields:

```text
id
expressionId
pattern
explanation
```

---

# 25. Tags and categories

Use tags to allow an expression to belong to several dimensions.

Examples:

```text
phrasal-verb
collocation
preposition
problem-solving
daily-conversation
software-development
work
meeting
B1
B2
```

Prefer tags over a single rigid category when possible.

---

# 26. Vocabulary packs

Content should be grouped into packs.

Example:

```text
Core English
Software Development
Daily Conversation
Work & Meetings
Travel
```

Initial distribution should prioritize the primary user's needs.

Suggested bundled packs:

```text
Core English
Developer English
```

Potential future packs:

```text
Daily Conversation
Work & Meetings
Travel
Reading
Technical Presentations
Interviews
```

---

# 27. Content pack metadata

Conceptual metadata:

```text
id
name
description
version
language
expressionCount
source
installedAt
updatedAt
bundled
```

This allows future downloading and updating without redesigning the database.

---

# 28. Seed data

The application should ship with enough content to be useful without internet.

Possible source structure:

```text
app/src/main/assets/content/
├── core-english.json
└── developer-english.json
```

Example:

```json
{
  "id": "figure-out",
  "expression": "figure out",
  "meaning": "averiguar, descubrir o resolver",
  "explanation": "Used when discovering an answer, understanding something, or solving a problem.",
  "patterns": [
    "figure + object + out",
    "figure out + wh-clause"
  ],
  "examples": [
    {
      "english": "I'm trying to figure out what happened.",
      "spanish": "Estoy intentando averiguar qué pasó.",
      "context": "general"
    },
    {
      "english": "We need to figure out why the service keeps failing.",
      "spanish": "Necesitamos averiguar por qué el servicio sigue fallando.",
      "context": "software-development"
    }
  ],
  "tags": [
    "phrasal-verb",
    "problem-solving"
  ],
  "level": "B1"
}
```

The exact JSON schema should be versioned.

Example:

```json
{
  "schemaVersion": 1,
  "pack": {},
  "expressions": []
}
```

---

# 29. Seed import

Suggested first-run behavior:

```text
Application start
      ↓
Check database
      ↓
Bundled content already imported?
 ┌────┴────┐
No        Yes
↓          ↓
Import     Continue
JSON
↓
Room
```

Import must be:

- idempotent;
- transactional where practical;
- version-aware;
- safe to retry.

Do not reinsert duplicate expressions on every app start.

---

# 30. Persistence entities

A reasonable initial relational model is:

```text
ContentPackEntity
ExpressionEntity
ExampleEntity
PatternEntity
TagEntity
ExpressionTagCrossRef
LearningStateEntity
ReviewEntity
LearningSessionEntity
SessionExerciseEntity (optional in V1)
```

Do not create tables that are not required by current use cases.

---

# 31. Learning state

Suggested conceptual fields:

```text
expressionId
stage
recognitionScore
productionScore
nextReviewAt
lastReviewedAt
reviewCount
successfulReviewCount
failedReviewCount
currentInterval
difficulty/ease parameters
updatedAt
```

The exact scheduling-specific fields should be isolated enough to allow a future scheduler migration.

---

# 32. Review history

Do not store only the latest score.

Historical review events are useful for:

- progress calculations;
- debugging the scheduler;
- adapting difficulty;
- detecting weak expressions;
- future migration to better algorithms.

Suggested review event:

```text
id
expressionId
sessionId
reviewType
rating
isCorrect
responseTimeMs
reviewedAt
previousStage
newStage
```

If an answer contains free text, storing it is optional and should have a clear product reason.

---

# 33. Review types

At minimum:

```kotlin
enum class ReviewType {
    RECOGNITION,
    CLOZE,
    GUIDED_RECALL,
    TRANSLATION,
    PRODUCTION,
}
```

Possible semantics:

### RECOGNITION

The expression is shown and the user identifies its meaning.

### CLOZE

Part of the phrase is omitted.

Example:

```text
I'm trying to ______ what happened.
```

### GUIDED_RECALL

The user receives stronger hints, for example:

```text
"Estoy intentando averiguar..."

Hint:
I'm trying to f______ o____
```

### TRANSLATION

The user receives a Spanish sentence/context and produces English.

### PRODUCTION

The user receives a situation or intent rather than a direct translation.

Example:

```text
Your service works locally but fails in production.

Explain that you are investigating the cause.
```

Possible answer:

```text
I'm trying to figure out why it fails in production.
```

---

# 34. Answer ratings

Suggested user-facing ratings:

```text
Forgot
Hard
Good
Easy
```

Internal representation should not depend directly on display strings.

Example:

```kotlin
enum class ReviewRating {
    FORGOT,
    HARD,
    GOOD,
    EASY,
}
```

The scheduler uses ratings to determine future review timing.

---

# 35. Answer evaluation strategy

V1 must not depend on an LLM.

Evaluation should be deterministic where possible.

Potential V1 strategies:

### Recognition

Exact selected answer.

### Cloze

Normalized comparison against accepted answers.

### Translation

Allow:

- primary expected answer;
- curated accepted variants;
- normalized punctuation/case differences.

### Production

V1 may use self-assessment after revealing one or more expected answers if deterministic automatic grading would be unreliable.

Example flow:

```text
Prompt
 ↓
User produces answer
 ↓
Reveal expected/natural answer
 ↓
User rates:
Forgot / Hard / Good / Easy
```

This avoids pretending that a simplistic exact-string matcher understands English semantics.

---

# 36. Text normalization

For deterministic comparisons, normalization may include:

- trim;
- case normalization where appropriate;
- normalized whitespace;
- normalized apostrophes;
- optional terminal punctuation handling.

Do not silently accept semantically incorrect answers merely because they have similar tokens.

Keep the evaluator simple and testable.

---

# 37. Learning engine

The `LearningEngine` is one of the core domain components.

Conceptual interface:

```kotlin
interface LearningEngine {
    suspend fun createDailySession(
        preferences: LearningPreferences,
    ): LearningSession

    suspend fun registerAnswer(
        exercise: Exercise,
        answer: UserAnswer,
    ): ReviewResult
}
```

Responsibilities may include:

- selecting due expressions;
- selecting new expressions;
- prioritizing weak expressions;
- balancing recognition and production;
- respecting session duration;
- choosing exercise difficulty;
- updating learning state.

Do not put these rules inside a screen ViewModel.

---

# 38. Daily session composition

A daily session should combine:

```text
Due reviews
+
Weak-expression practice
+
Production practice
+
New expressions
```

Initial prioritization:

```text
1. Overdue reviews
2. Weak expressions
3. Productive recall
4. New content
```

A user should not accumulate unlimited new material while ignoring scheduled reviews.

---

# 39. Session time estimation

Exercises may have estimated costs.

Example:

```text
Recognition:     15 sec
Cloze:           25 sec
Guided recall:   30 sec
Translation:     45 sec
Production:      60 sec
New expression:  90 sec
```

These are initial heuristics, not permanent constants.

The application should eventually refine estimates using real session data.

The engine should build a session close to the configured daily target.

---

# 40. Personalization without AI

Before introducing an LLM, the application can already personalize sessions using local statistics.

Examples:

```text
phrasal verbs       weak
prepositions        weak
technical English   strong
recognition          strong
production           weak
```

The learning engine may respond by increasing appropriate exercises.

This should be implemented using deterministic rules before introducing machine learning.

---

# 41. Initial personalization rules

Possible rules:

```text
If productionScore << recognitionScore
    increase production exercises

If tag accuracy < threshold
    increase exercises for that tag

If expression failed repeatedly
    shorten review interval
    temporarily add guided recall

If expression remains stable for long intervals
    reduce frequency

If overdue reviews are high
    reduce new expressions
```

Thresholds should initially be configurable constants in the domain layer.

Do not bury unexplained numeric values throughout the code.

---

# 42. Onboarding

Onboarding must work offline.

Initial questions:

## Learning goal

Possible options:

```text
General English
Reading
Technical English
Conversation
```

For the initial personal version, multiple goals may be allowed, with an initial default toward:

```text
General English
Technical English
```

## Daily duration

```text
15
30
45
60 minutes
```

## Initial content

Allow selection of bundled packs.

Example:

```text
[x] Core English
[x] Developer English
```

## Initial assessment

A future early milestone may include a short offline placement/diagnostic test.

Do not block V1 completion on building a sophisticated CEFR placement test.

---

# 43. Screens

## 43.1 OnboardingScreen

Responsibilities:

- explain the learning approach;
- select daily goal;
- select relevant content;
- save preferences;
- initialize first learning session.

---

## 43.2 HomeScreen

Purpose:

> Make today's next action obvious.

Example:

```text
Good afternoon

Today's goal
████████░░ 32 / 45 min

Due reviews
23 expressions

New expressions
5

Recognition
87%

Production
63%

[ Start today's session ]
```

Possible secondary actions:

```text
Continue session
Review weak expressions
Open library
View progress
```

Avoid clutter.

---

## 43.3 LearnScreen

Introduces a new expression.

Example:

```text
FIGURE OUT

"I'm trying to figure out why
the application keeps crashing."

figure something out

averiguar / descubrir / resolver algo

Pattern:
figure + object + out

Examples:
• I can't figure it out.
• Did you figure out what happened?
• We need to figure out why it failed.

[ I understand ]
```

Marking "I understand" means the expression has been introduced, **not mastered**.

---

## 43.4 PracticeScreen

The main review/exercise screen.

It should support multiple exercise renderers based on `ReviewType`.

Example:

```text
Estoy intentando averiguar por qué falla.

How would you say it in English?

[_________________________________]

[ Check ]
```

After evaluation:

```text
Expected:
I'm trying to figure out why it's failing.

[ Forgot ] [ Hard ] [ Good ] [ Easy ]
```

---

## 43.5 SessionSummaryScreen

At the end of a session:

```text
Session complete

Duration: 43 min
Reviewed: 31
New expressions: 5
Recognition accuracy: 89%
Production accuracy: 68%
Expressions improved: 12

Weak area:
phrasal verbs
```

Avoid meaningless celebratory metrics.

---

## 43.6 LibraryScreen

Local searchable library.

Features:

- search;
- filters;
- expression state;
- tags;
- packs;
- detail navigation.

Suggested filters:

```text
All
New
Learning
Weak
Productive
Mastered
```

---

## 43.7 ExpressionDetailScreen

Display:

- expression;
- meaning;
- explanation;
- pattern;
- examples;
- tags;
- current state;
- recognition score;
- production score;
- review history summary;
- next scheduled review.

Future versions may allow adding/editing personal examples.

---

## 43.8 ProgressScreen

Metrics should emphasize actual learning.

Examples:

```text
Expressions seen
Expressions recognized
Expressions recallable
Expressions productive
Expressions mastered

Recognition accuracy
Production accuracy

Minutes studied this week
Reviews completed
New expressions introduced
```

Useful breakdowns:

```text
By tag
By content pack
By exercise type
By week
```

Possible weak-area view:

```text
Phrasal verbs        58%
Prepositions         62%
Collocations         72%
Technical English    84%
```

---

## 43.9 SettingsScreen

Settings may include:

```text
Daily goal
New expressions/day maximum
Selected content packs
Theme
Learning preferences
Reset progress
Export data (future)
About
```

Destructive actions must require confirmation.

---

# 44. Navigation

Suggested navigation graph:

```text
Onboarding
   ↓
Home
 ├── Learn
 ├── Practice
 │    └── SessionSummary
 ├── Library
 │    └── ExpressionDetail
 ├── Progress
 └── Settings
```

Use typed routes where supported and appropriate.

Do not pass entire domain objects through navigation arguments.

Pass stable identifiers such as:

```text
expressionId
sessionId
```

and load state from repositories.

---

# 45. UI state management

Prefer unidirectional data flow.

Example:

```text
UI Event
   ↓
ViewModel
   ↓
Use Case / Domain
   ↓
Repository
   ↓
StateFlow
   ↓
Composable
```

Screen state examples:

```kotlin
sealed interface PracticeUiState {
    data object Loading : PracticeUiState

    data class Active(
        val exercise: ExerciseUiModel,
        val progress: SessionProgressUiModel,
    ) : PracticeUiState

    data class Completed(
        val summary: SessionSummaryUiModel,
    ) : PracticeUiState

    data class Error(
        val message: String,
    ) : PracticeUiState
}
```

Do not use mutable application state directly from composables.

---

# 46. Coroutines and Flow

Use Kotlin coroutines for asynchronous work.

Prefer `Flow`/`StateFlow` for observable persistent state.

Avoid unnecessary RxJava introduction.

Database and DataStore access must not block the main thread.

---

# 47. Dependency injection

Dependency injection is useful but should not dominate V1.

A reasonable choice is Hilt if the project benefits from standard Android integration.

Alternatively, manual dependency injection is acceptable while the graph is small.

Whichever approach is selected:

- repositories must be replaceable in tests;
- schedulers/evaluators must be injectable;
- database construction should be centralized;
- avoid global service locators.

---

# 48. Database migrations

Never use destructive migration as the normal strategy once meaningful user learning data exists.

Learning history is valuable and must survive application upgrades.

Requirements:

- schema versioning;
- explicit migrations;
- migration tests;
- backup/export consideration for future versions.

Seed content updates and database schema migrations are separate concerns.

---

# 49. Local search

For V1, standard Room/SQLite queries are likely sufficient.

Search may cover:

```text
expression
meaning
example text
tag
```

Do not introduce Elasticsearch or external search services.

SQLite FTS may be considered later if the library becomes large enough and profiling justifies it.

---

# 50. Security and privacy

The application should collect no data that it does not need.

V1 requires no account and no network service.

Learning data remains local on the device.

If future AI/cloud/sync features are introduced:

- they must be explicit;
- permissions/data sharing must be transparent;
- offline functionality must remain available where possible.

Do not request Android permissions that V1 does not use.

---

# 51. AI policy

V1 must be fully functional without an LLM.

AI is an optional future enhancement.

Correct relationship:

```text
Core learning app
      │
      ├── Rule-based implementation
      │
      └── Optional AI enhancement
```

Incorrect relationship:

```text
LLM unavailable
    ↓
Application unusable
```

---

# 52. Future LearningAssistant abstraction

Prepare architectural boundaries for a future assistant without implementing it prematurely.

Concept:

```kotlin
interface LearningAssistant {

    suspend fun generateExamples(
        expression: Expression,
    ): List<Example>

    suspend fun generateExercise(
        expression: Expression,
        context: LearningContext,
    ): Exercise

    suspend fun evaluate(
        exercise: Exercise,
        answer: UserAnswer,
    ): Evaluation
}
```

Initial implementation could be:

```text
RuleBasedLearningAssistant
```

Future implementation:

```text
OnDeviceLearningAssistant
```

Do not create fake AI abstractions that have no current consumer. Introduce interfaces when the first concrete use case requires them.

---

# 53. Future on-device AI

Possible future uses:

- generate sentence variations;
- create contextual prompts;
- create examples related to software development;
- explain errors;
- generate paraphrases;
- evaluate free-text production;
- adapt exercises to weaknesses.

On-device inference is preferred over mandatory cloud inference for the personal/offline product direction.

However:

- model availability varies by device;
- memory/storage requirements vary;
- latency varies;
- model quality varies;
- unsupported devices must retain the rule-based experience.

AI must therefore be capability-detected.

---

# 54. Future listening milestone

Listening is not part of V1.

Potential later features:

```text
audio → identify expression
dictation
audio + transcript
connected-speech practice
minimal pairs
shadowing
speed-adjusted playback
```

Possible learning flow:

```text
Listen without text
      ↓
Attempt comprehension
      ↓
Reveal transcript
      ↓
Listen with transcript
      ↓
Listen again without text
```

Audio assets should be downloadable packs if application size becomes an issue.

---

# 55. Future speaking milestone

Speaking is not part of V1.

Potential capabilities:

- speech recognition;
- guided production;
- shadowing;
- pronunciation comparison;
- conversational prompts;
- role-play;
- automatic feedback.

Do not mix speaking implementation into the initial vocabulary engine.

---

# 56. Metrics

The product should measure useful outcomes.

## Important metrics

```text
study minutes
reviews completed
new expressions introduced
recognition success
production success
expressions by learning stage
overdue reviews
retention by interval
weak tags
session completion rate
```

## Secondary metrics

```text
daily streak
number of sessions
```

Streaks should never become the primary measure of learning.

---

# 57. Progress model

Useful dashboard:

```text
Active vocabulary
142 expressions

Recognition
87%

Production
63%

Mastered this week
+27

Study time
4h 12m
```

More useful:

```text
Weak areas

Phrasal verbs       58%
Prepositions        62%
Conditionals        69%
Collocations        72%
```

Progress calculations must be derived from persisted learning/review data, not manually stored duplicated counters unless performance requires materialization.

---

# 58. Testing strategy

The learning engine is the most important code to test.

## Unit tests

Prioritize:

```text
LearningState transitions
ReviewScheduler behavior
Daily session composition
Weak-expression prioritization
Recognition/production scoring
Exercise generation
Answer normalization/evaluation
Time-target calculation
Seed-content validation
```

Example:

```text
Given:
- daily target = 30 min
- 20 overdue reviews
- 5 new expressions/day

When:
createDailySession()

Then:
- overdue reviews are prioritized
- session is near target duration
- new expressions do not displace critical overdue reviews
```

---

# 59. Database tests

Test:

- inserts;
- relations;
- transactions;
- due-review queries;
- learning-state updates;
- migration paths;
- seed import idempotency;
- duplicate handling.

Use Android instrumentation only where Android/Room behavior requires it.

---

# 60. UI tests

Do not attempt exhaustive UI automation in the first commit.

Prioritize critical flows:

```text
Onboarding → Home
Home → Start session
Learn expression
Complete review
Finish session
View progress
```

Composable previews should be added for important UI states.

---

# 61. Content validation

Bundled vocabulary is product data and must be validated.

Build a validator that checks:

```text
unique expression IDs
non-empty expression
non-empty meaning
at least one example
valid tag references
valid CEFR values
valid pack references
schemaVersion compatibility
duplicate examples
```

Prefer validation at build/test time in addition to defensive runtime validation.

---

# 62. Error handling

Core learning should remain usable after non-critical errors.

Examples:

### Seed import failure

Display a recoverable local error and allow retry.

### Corrupt individual content item

Skip/report invalid item where safe rather than corrupting the entire dataset.

### Database failure

Expose meaningful application state and diagnostics; do not silently reset user progress.

### Future network failure

Do not interrupt offline features.

---

# 63. Logging

Use structured development logging where useful.

Do not log:

- sensitive future user content unnecessarily;
- full large datasets;
- every database operation in production.

Important events may include:

```text
seed import started/completed/failed
session generated
database migration completed/failed
pack imported
```

---

# 64. Accessibility

Compose UI should consider:

- readable text sizes;
- semantic labels;
- TalkBack;
- sufficient touch targets;
- dynamic font scaling;
- clear state indicators not dependent only on color.

Learning content must remain readable under font scaling.

---

# 65. Design principles

The interface should be:

- simple;
- distraction-free;
- text-first;
- fast;
- usable one-handed where practical;
- focused on completing today's session.

Avoid turning the application into a game dashboard.

Animations are welcome only when they improve clarity.

---

# 66. Performance goals

V1 should feel instantaneous for normal usage.

Targets:

- app startup without unnecessary network wait;
- library search responsive for bundled datasets;
- exercise transitions immediate;
- database operations off main thread;
- session generation fast enough to appear immediate.

Do not optimize speculatively.

Profile before introducing complex caches or denormalization.

---

# 67. Content quantity

Do not begin by authoring 10,000 expressions.

Start with a small high-quality dataset sufficient to validate the learning experience.

Suggested implementation dataset:

```text
50–100 expressions
```

Then expand to an initial personal-use dataset:

```text
Core English:       ~500–1000 useful expressions
Developer English:  ~200–500 expressions
```

Quality and usefulness are more important than raw count.

---

# 68. Content selection principle

Prefer an expression when the user is likely to:

- encounter it frequently;
- use it personally;
- reuse its pattern;
- need it for reading/speaking;
- confuse it with another construction;
- benefit from contextual examples.

Do not select vocabulary merely because it is obscure or difficult.

For the initial user, prioritize:

- daily conversational English;
- software-development English;
- problem solving;
- explaining decisions;
- asking questions;
- technical discussions;
- describing causes/effects;
- expressing uncertainty;
- comparing approaches;
- meetings/work communication.

---

# 69. Recommended initial content examples

Examples of useful chunks:

```text
figure out
turn out
deal with
run into an issue
come up with
take into account
as far as I know
it depends on
I'm not sure whether...
I'm trying to...
I'm used to...
I'm not used to...
the main reason is...
from my point of view...
there's no need to...
it makes sense to...
as long as...
in order to...
```

These examples are illustrative and not a complete pack.

---

# 70. V1 user journey

The desired end-to-end experience:

```text
Install app
   ↓
Onboarding
   ↓
Choose 45 min/day
   ↓
Choose Core + Developer English
   ↓
Home
   ↓
Start today's session
   ↓
Review due expressions
   ↓
Learn a few new expressions
   ↓
Perform recognition exercises
   ↓
Perform recall/production exercises
   ↓
Rate difficulty
   ↓
Complete session
   ↓
See meaningful summary
   ↓
Close app
   ↓
Return tomorrow
```

The application should remember everything locally.

---

# 71. Definition of V1 success

V1 is successful when all of these are true:

- it can be installed and used entirely offline;
- onboarding completes without a server;
- bundled expressions import correctly;
- a daily session is generated;
- the user can learn a new expression;
- reviews are scheduled;
- recognition and production are measured separately;
- completing an exercise affects future scheduling;
- closing/reopening the application preserves progress;
- the next day produces appropriate due reviews;
- progress reflects persisted history;
- no AI model is necessary;
- no backend is necessary.

---

# 72. V1 acceptance criteria

## Offline

```text
GIVEN airplane mode is enabled
WHEN the user opens the application
THEN all core learning functionality remains available
```

## Persistence

```text
GIVEN the user completes exercises
WHEN the application is killed and reopened
THEN learning progress is preserved
```

## Scheduling

```text
GIVEN an expression is reviewed
WHEN the user rates the review
THEN a future review date is calculated and persisted
```

## Productive vocabulary

```text
GIVEN an expression has high recognition success
AND low production success
WHEN a daily session is generated
THEN production-oriented exercises should receive additional priority
```

## Seed content

```text
GIVEN bundled data was already imported
WHEN the application starts again
THEN the importer must not create duplicates
```

## Time goal

```text
GIVEN the configured goal is 45 minutes
WHEN the daily session is generated
THEN the estimated session should reasonably approximate that duration
```

---

# 73. Roadmap

## Phase 0 — Project foundation

Goal:

Create a reliable Android project foundation.

Tasks:

```text
Create Android project
Configure Kotlin
Configure Compose
Configure Material 3
Configure version catalog
Configure Navigation
Configure dependency injection strategy
Configure test frameworks
Create basic modules/packages
Create CI build
```

Deliverable:

```text
App launches
Navigation skeleton works
Tests run
```

---

## Phase 1 — Domain model

Goal:

Model the learning problem before building screens.

Tasks:

```text
Define Expression
Define Example
Define Pattern
Define Tag
Define LearningState
Define Review
Define ReviewType
Define ReviewRating
Define LearningStage
Define LearningSession
Define Exercise
Define repository contracts
```

Deliverable:

```text
Pure Kotlin domain model
No Room annotations in domain
Unit-testable model
```

---

## Phase 2 — Local persistence

Goal:

Create the offline source of truth.

Tasks:

```text
Configure Room
Create entities
Create DAOs
Create relations
Create database
Create repository implementations
Configure DataStore
Write database tests
Define migration strategy
```

Deliverable:

```text
Expressions and learning state persist locally
```

---

## Phase 3 — Content system

Goal:

Bundle useful starter content.

Tasks:

```text
Define versioned JSON schema
Create Core English sample pack
Create Developer English sample pack
Build JSON parser
Build validator
Build idempotent importer
Persist packs in Room
Test invalid/duplicate content
```

Deliverable:

```text
50–100 validated expressions available offline
```

---

## Phase 4 — Review scheduler

Goal:

Implement deterministic spaced repetition.

Tasks:

```text
Define ReviewScheduler
Implement initial scheduler
Record review history
Calculate nextReviewAt
Handle Forgot/Hard/Good/Easy
Test intervals and edge cases
```

Deliverable:

```text
Reviewed expressions reappear at appropriate times
```

---

## Phase 5 — Exercise engine

Goal:

Turn content into practice.

Tasks:

```text
Implement Recognition exercise
Implement Cloze exercise
Implement Guided Recall
Implement Translation
Implement Production/self-rating
Implement normalization
Implement AnswerEvaluator
Test exercise generation
```

Deliverable:

```text
An expression can be practiced at multiple retrieval levels
```

---

## Phase 6 — Learning engine

Goal:

Build personalized daily sessions.

Tasks:

```text
Select overdue expressions
Select weak expressions
Select new expressions
Balance recognition/production
Respect daily duration
Limit new content when reviews accumulate
Generate LearningSession
Persist session progress
```

Deliverable:

```text
One call can generate today's useful session
```

---

## Phase 7 — Onboarding and Home

Goal:

Make the application usable from first launch.

Tasks:

```text
Create onboarding flow
Configure daily duration
Select packs
Persist preferences
Create HomeScreen
Display due/new counts
Start today's session
```

Deliverable:

```text
Fresh install → onboarding → home → start session
```

---

## Phase 8 — Learning and practice UI

Goal:

Complete the primary learning loop.

Tasks:

```text
Create LearnScreen
Create PracticeScreen
Create exercise-specific composables
Create rating controls
Create progress indicator
Create SessionSummaryScreen
```

Deliverable:

```text
User can complete an entire daily session
```

---

## Phase 9 — Library and progress

Goal:

Expose useful feedback.

Tasks:

```text
Create LibraryScreen
Implement local search
Implement filters
Create ExpressionDetailScreen
Create ProgressScreen
Calculate stage counts
Calculate recognition/production metrics
Calculate weak tags
```

Deliverable:

```text
User can inspect what is improving and what remains weak
```

---

## Phase 10 — Hardening

Goal:

Make V1 dependable.

Tasks:

```text
Migration tests
Process-death tests
Import edge cases
Accessibility review
Performance profiling
UI polish
Error-state review
Offline verification
Release build
```

Deliverable:

```text
Usable personal V1
```

---

# 74. Post-V1 roadmap

## V1.5 — Better personalization

Potential work:

- better weakness detection;
- adaptive exercise ratios;
- refined session-duration estimation;
- improved scheduler;
- FSRS evaluation;
- personal content creation;
- import/export;
- content-pack management.

No AI required.

---

## V2 — On-device intelligent practice

Potential work:

- optional local model;
- contextual examples;
- paraphrases;
- semantic answer evaluation;
- personalized scenario generation;
- explanations.

Core app remains usable without AI.

---

## V3 — Listening

Potential work:

- audio packs;
- dictation;
- transcript reveal;
- connected-speech exercises;
- shadowing;
- playback controls.

---

## V4 — Speaking

Potential work:

- speech recognition;
- guided speaking;
- pronunciation feedback;
- role-play;
- conversation sessions.

---

# 75. Technical decision log

Record meaningful architectural decisions in:

```text
docs/adr/
```

Examples:

```text
0001-single-activity-compose.md
0002-room-as-local-source-of-truth.md
0003-no-llm-in-v1.md
0004-expression-not-word-as-core-entity.md
0005-recognition-and-production-are-separate.md
```

An ADR is warranted for decisions that future contributors might otherwise undo without understanding why they were made.

---

# 76. Suggested documentation layout

```text
README.md
docs/
├── architecture.md
├── learning-model.md
├── content-format.md
├── testing.md
├── roadmap.md
└── adr/
```

The README may initially contain everything in this document.

As implementation grows, move detailed sections into `docs/` and keep the README as the entry point.

Do not duplicate conflicting documentation.

---

# 77. Coding guidelines

Prefer:

- idiomatic Kotlin;
- immutable domain models;
- explicit domain names;
- small focused functions;
- composition;
- testable business rules;
- sealed types where states are finite;
- value classes where IDs benefit from type safety;
- coroutines/Flow;
- constructor injection.

Avoid:

- giant ViewModels;
- generic `Utils` classes;
- mutable global state;
- nullable-everything models;
- business logic in composables;
- direct DAO access from UI;
- premature generic frameworks;
- abstractions with only hypothetical consumers.

---

# 78. Naming guidelines

Use domain terminology consistently.

Preferred:

```text
Expression
LearningState
LearningStage
Review
ReviewRating
ReviewType
Exercise
LearningSession
ReviewScheduler
LearningEngine
ContentPack
```

Avoid vague names such as:

```text
Item
Data
Manager
Helper
Thing
WordData
Utils
```

unless their responsibility is genuinely clear.

---

# 79. Git strategy

Small, coherent commits are preferred.

Example initial history:

```text
chore: initialize android compose project
chore: configure version catalog and quality tools
feat: add learning domain models
feat: add room persistence
feat: add bundled content importer
feat: add review scheduler
feat: add exercise engine
feat: add daily session generator
feat: add onboarding
feat: add practice flow
feat: add progress tracking
```

Do not generate the entire application in one unreviewable commit.

---

# 80. CI

Initial CI should at minimum run:

```text
Gradle build
Unit tests
Lint
```

Later:

```text
Instrumentation tests
Database migration tests
Static analysis
Release build verification
```

The app should remain buildable from a clean checkout.

---

# 81. Definition of done for a feature

A feature is not done merely because a screen renders.

For domain-changing features, require:

- behavior implemented;
- relevant tests;
- persistence if required;
- error state;
- loading state if relevant;
- accessibility basics;
- no regression to offline operation;
- documentation updated if architecture/content format changes.

---

# 82. Implementation constraints for coding agents

When using OpenCode, Claude Code, Codex, or another coding agent, follow these rules.

## Do

1. Read this README before modifying architecture.
2. Inspect existing code before creating new abstractions.
3. Work phase by phase.
4. Keep the application compilable after each task.
5. Prefer stable Android/Kotlin APIs.
6. Add tests for domain rules.
7. Preserve offline-first behavior.
8. Preserve recognition/production distinction.
9. Preserve `Expression` as the core learning concept.
10. Use Room as local source of truth.
11. Keep AI optional.
12. Explain architectural deviations.
13. Update documentation when a decision changes.

## Do not

1. Add a backend for V1.
2. Add authentication.
3. Add an online LLM dependency.
4. create one Activity per screen.
5. access Room directly from composables.
6. build custom cache infrastructure without evidence.
7. replace contextual expressions with isolated vocabulary as the main model.
8. mark an expression mastered based only on recognition.
9. introduce preview dependencies without justification.
10. create dozens of Gradle modules prematurely.
11. implement future listening/speaking work while the vocabulary engine is incomplete.
12. erase learning data through destructive migrations.

---

# 83. Recommended agent workflow

A coding agent should not immediately implement every section.

Recommended workflow:

```text
1. Inspect repository
2. Compare current state with this specification
3. Create implementation backlog
4. Identify Phase 0 tasks
5. Implement one small vertical increment
6. Run tests/build
7. Report result
8. Continue to next task
```

For each phase, produce tasks with:

```text
Title
Goal
Files/modules affected
Dependencies
Acceptance criteria
Tests
```

Example:

```text
Task:
Implement Expression domain model

Goal:
Represent reusable lexical chunks independently from persistence.

Acceptance criteria:
- Expression has stable ID
- Supports multiple examples
- Supports multiple patterns
- Supports multiple tags
- Contains no Android/Room annotations
- Unit tests compile

Depends on:
None
```

---

# 84. Recommended first vertical slice

After project setup, prefer building one complete thin slice rather than all layers independently.

Example:

```text
Bundled "figure out" expression
        ↓
Imported into Room
        ↓
Loaded through repository
        ↓
Displayed in LearnScreen
        ↓
Reviewed in simple exercise
        ↓
Review stored
        ↓
Next review scheduled
        ↓
Progress survives restart
```

Once this flow works, generalize it to more content.

This validates the architecture earlier than building twenty empty interfaces.

---

# 85. First implementation milestone

The first meaningful milestone should demonstrate:

```text
1 expression
1 content pack
1 Room database
1 learning state
1 review type
1 scheduler
1 learn screen
1 practice screen
1 persisted result
```

Expected demo:

```text
Launch app
→ see "figure out"
→ learn it
→ perform exercise
→ rate review
→ close app
→ reopen app
→ progress still exists
```

Only after this works should implementation expand horizontally.

---

# 86. Questions that must remain explicit during development

When making future decisions, ask:

### Learning value

Does this feature improve acquisition, recall, production, or retention?

### Offline compatibility

Does this create an unnecessary network dependency?

### Complexity

Can the same learning outcome be achieved more simply?

### Measurement

How will we know whether this improves learning?

### Maintainability

Can the learning algorithm change without rewriting the UI/database?

### Personal usefulness

Will the primary user actually use this during a 30–60 minute daily session?

If the answer is unclear, prefer the simpler implementation.

---

# 87. Long-term product vision

The project may eventually become a fully personal offline English tutor:

```text
Vocabulary / Lexical chunks
        ↓
Reading
        ↓
Listening
        ↓
Speaking
        ↓
Context-aware practice
        ↓
On-device tutor
```

However, every stage should remain useful by itself.

The first version should already be valuable without waiting for AI, listening, or speaking.

---

# 88. Core principle to preserve

The most important product rule is:

> **The application is not trying to count how many English words the user has seen. It is trying to convert useful expressions into language the user can retrieve and produce.**

Every major feature should support that goal.

---

# 89. Immediate next step

Before writing production features, the coding agent should:

1. inspect the repository;
2. establish the Android project baseline;
3. propose the exact module/package layout based on the existing repository;
4. produce a task backlog for **Phase 0 through Phase 2**;
5. identify the smallest vertical slice described above;
6. implement only after the repository structure and dependency choices are understood.

If the repository is empty, begin with Phase 0 and keep the first project structure intentionally small.

---

# 90. Initial implementation checklist

```text
[ ] Android project builds
[ ] Kotlin configured
[ ] Compose configured
[ ] Material 3 configured
[ ] Version catalog configured
[ ] Single MainActivity
[ ] Compose Navigation configured
[ ] Domain models created
[ ] Room configured
[ ] DataStore configured
[ ] Seed JSON schema defined
[ ] Seed importer implemented
[ ] First content pack imported
[ ] ReviewScheduler implemented
[ ] Review history persisted
[ ] Exercise model implemented
[ ] Recognition exercise implemented
[ ] Production/self-rating exercise implemented
[x] LearningEngine implemented
[x] Daily session generation implemented
[ ] Onboarding implemented
[ ] Home implemented
[ ] Learn flow implemented
[ ] Practice flow implemented
[ ] Session summary implemented
[ ] Library implemented
[ ] Progress implemented
[ ] Offline behavior verified
[ ] Unit tests passing
[ ] Database tests passing
[ ] Migration strategy verified
[ ] Release build succeeds
```

---

## Final implementation directive

Build the smallest complete offline learning loop first.

Do **not** optimize for feature count.

Optimize for this loop:

```text
Discover expression
      ↓
Understand expression in context
      ↓
Recall expression
      ↓
Produce expression
      ↓
Evaluate difficulty
      ↓
Schedule review
      ↓
Measure improvement
      ↓
Repeat tomorrow
```

If this loop is reliable, useful, fast, and pleasant enough to use every day for 30–60 minutes, the foundation is successful.
