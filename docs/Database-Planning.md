# Library Lane Database Planning

## Philosophy

Library Lane is designed around the concept of:

One User
One Personal Library
Unlimited Reading Experiences

A book exists once in the library.

Each book can contain unlimited reading experiences.

This allows users to document rereads, relistens, format changes, and evolving opinions over time.
## Planned V1 Entities

User
Book
Author
Series
Collection
ReadingExperience
Quote
MusicEntry
Goal
Genre
Tag
## Core Relationships

User
  └── Books

Book
  └── Reading Experiences

Book
  └── Quotes

Book
  └── Music Entries

Book
  └── Collections

Series
  └── Books


## Core Rule: One Book, Unlimited Reading Experiences

Library Lane should store each book once in a user's library and allow unlimited reading experiences to be attached to that book.

A reading experience represents one specific read, reread, listen, relisten, or DNF attempt. Users should be able to document each experience separately, including format, progress, dates, rating, thoughts, quotes, notes, and music.

This allows a favorite book to remain one visible book on the shelf while preserving the full history of every time the user returned to it.
# Library Lane Database Planning

## 1. Purpose of This Document

This document defines the planned database architecture for the portfolio version of Library Lane.

Library Lane is not a simple book CRUD application. It is a personal library, reading journal, music association, goal tracking, and analytics platform. Because of that, the database must be designed carefully before backend code is written.

The goal of this document is to define the Version 1 database foundation while leaving room for future features such as mobile scanning, advanced shelf customization, personalized recommendations, fan art, and family libraries.

---

# 2. Database Philosophy

Library Lane is built around the idea that reading is an experience, not just a record.

The database should support:

- Personal libraries
- Books
- Authors
- Genres
- Series
- Collections
- Unlimited reading experiences
- Quotes
- Journal entries
- Music associations
- Goals
- Analytics
- Future recommendations
- Future mobile app usage

The database should avoid treating a book as a single flat object with one status and one rating.

Instead, Library Lane should separate:

1. The book itself
2. The user’s relationship with the book
3. Each reading experience connected to that book
4. Personal content such as quotes, notes, music, and ratings

---

# 3. Core Rule: One Book, Unlimited Reading Experiences

This is one of the most important database rules in Library Lane.

A book should appear once in a user’s library, but it can have unlimited reading experiences attached to it.

Example:

```text
Fourth Wing
├── Reading Experience #1: Physical read, 2026, 5 stars
├── Reading Experience #2: Audiobook listen, 2027, 4.5 stars
├── Reading Experience #3: Reread, 2028, 5 stars
└── Reading Experience #4: Kindle reread, 2030, notes added

There should be no limit on the number of reading experiences.

This supports users who reread favorite books many times and want to document each experience separately.

Each reading experience may have its own:

Format
Status
Start date
Finish date
Progress
Rating
Review
Notes
Quotes
Music
DNF reason
Thoughts

This allows one book to remain one visible book on the shelf while preserving the full history of every time the user returned to it.

4. V1 Entity List

The planned Version 1 database should include these core entities:

User
Book
Author
Genre
Series
Collection
ReadingExperience
Quote
JournalEntry
MusicEntry
Goal

Future entities may include:

Tag
Trope
Theme
Decoration
ShelfLayout
ReadingProgressLog
Recommendation
FanArt
FamilyLibrary
MobileScanHistory

These future entities should not be built until needed.

5. Entity: User
Purpose

The User entity represents the person using Library Lane.

For Version 1:

One user = one personal library

Each user owns their own books, collections, goals, notes, quotes, music entries, and reading experiences.

Suggested Fields
id
email
username
displayName
passwordHash
role
createdAt
updatedAt
Notes

The user should not share data with other users in V1.

Family libraries, shared shelves, and public profiles are future features.

6. Entity: Book
Purpose

The Book entity represents the book/story itself.

This is the general book metadata, not the user’s personal reading experience.

A Book should not store personal rating, reading status, or user thoughts directly.

Suggested Fields
id
title
subtitle
description
publicationYear
publisher
pageCount
isbn10
isbn13
language
coverImageUrl
externalBookId
externalSource
createdAt
updatedAt
Notes

External sources may include:

Open Library
Google Books
Manual Entry

The user should always be able to manually edit or override information.

7. Entity: Author
Purpose

Authors should be stored separately so Library Lane can support flexible analytics.

Example analytics:

Most-read author
Highest-rated author
Most-reread author
Author with most TBR books
Suggested Fields
id
name
bio
externalAuthorId
createdAt
updatedAt
Relationship

A book can have multiple authors.

An author can have multiple books.

This is a many-to-many relationship.

Book many-to-many Author
8. Entity: Genre
Purpose

Genres help with organization, filtering, and analytics.

Example analytics:

Most-read genre
Highest-rated genre
Lowest-rated genre
Most-DNF’d genre
Most-owned genre
Suggested Fields
id
name
createdAt
updatedAt
Relationship

A book can have multiple genres.

A genre can belong to many books.

Book many-to-many Genre
Notes

External APIs may return messy genres/categories. Later, Library Lane may need genre normalization.

Example:

Juvenile Fiction
Young Adult Fiction
Fantasy
Romance
Dragons

These may need to be cleaned into user-friendly categories later.

9. Entity: Series
Purpose

Many readers care deeply about series tracking.

Series support allows Library Lane to track:

Series name
Book number
Series completion
Next unread book
Series rating
Series music
Series notes
Suggested Fields
id
name
description
totalBooks
status
createdAt
updatedAt

Possible status values:

ONGOING
COMPLETED
UNKNOWN
Relationship

A series can have many books.

A book may belong to one series.

Series one-to-many Book

Book should include:

seriesOrderNumber
10. Entity: Collection
Purpose

Collections represent custom bookshelves.

A collection is not just a filter. It is a curated shelf.

Examples:

2026 Reads
Summer TBR
Fantasy Favorites
Comfort Books
Audiobooks I Loved
Rereads
Books That Made Me Cry
Suggested Fields
id
userId
name
description
theme
sortOrder
createdAt
updatedAt
Relationship

A user can have many collections.

A book can belong to many collections.

A collection can contain many books.

User one-to-many Collection
Collection many-to-many Book

The join table should eventually support manual ordering:

collection_books
- collection_id
- book_id
- position
11. Entity: ReadingExperience
Purpose

ReadingExperience is one of the most important entities in Library Lane.

It represents one specific read, reread, listen, relisten, DNF attempt, or future reading attempt.

A book can have unlimited reading experiences.

Suggested Fields
id
userId
bookId
experienceNumber
experienceType
format
status
startDate
finishDate
currentPage
totalPages
percentComplete
currentListeningSeconds
totalListeningSeconds
listeningSpeed
rating
review
dnfReason
summaryThoughts
createdAt
updatedAt
Experience Types

Possible values:

FIRST_READ
REREAD
FIRST_LISTEN
RELISTEN
DNF_ATTEMPT
OTHER
Formats
PHYSICAL
EBOOK
AUDIOBOOK
Statuses
TBR
CURRENTLY_READING
COMPLETED
DNF
PAUSED

V1 may use:

TBR
CURRENTLY_READING
COMPLETED
DNF
Notes

A user can read the same book 20 times and each experience should be stored separately.

Each experience can have its own:

Rating
Thoughts
Quotes
Music
Notes
Format
Progress
12. Entity: Quote
Purpose

Quotes allow users to save meaningful lines from books.

Quotes should connect to both:

the book
optionally a specific reading experience
Suggested Fields
id
userId
bookId
readingExperienceId
quoteText
pageNumber
chapter
timestampSeconds
speaker
note
spoiler
favorite
createdAt
updatedAt
Notes

For audiobooks, timestampSeconds may be more useful than pageNumber.

Quotes should eventually be searchable.

13. Entity: JournalEntry
Purpose

Journal entries store personal thoughts, reflections, theories, reviews, and reread observations.

Suggested Fields
id
userId
bookId
readingExperienceId
title
body
entryType
pageNumber
chapter
timestampSeconds
spoiler
createdAt
updatedAt
Entry Types

Possible values:

GENERAL
REVIEW
CHAPTER_NOTE
SCENE_NOTE
THEORY
REREAD_OBSERVATION
CHARACTER_NOTE
TROPE_NOTE
EMOTIONAL_REACTION
Notes

Journal entries should be optional.

Some users may write long reflections.

Some may never journal.

Both should be supported.

14. Entity: MusicEntry
Purpose

MusicEntry stores songs, playlists, albums, or music links associated with a book, series, reading experience, scene, character, or mood.

Music is part of Library Lane’s emotional identity.

Suggested Fields
id
userId
bookId
seriesId
readingExperienceId
title
artist
playlistName
platform
musicType
embedUrl
externalUrl
associationType
associationDescription
notes
createdAt
updatedAt
Music Types
SONG
PLAYLIST
ALBUM
ARTIST
OTHER
Platforms
SPOTIFY
APPLE_MUSIC
YOUTUBE
SOUNDCLOUD
OTHER
Association Types
WHOLE_BOOK
SERIES
READING_EXPERIENCE
CHAPTER
SCENE
CHARACTER
RELATIONSHIP
MOOD
TROPE
READING_MEMORY
GENERAL_VIBE
Notes

For V1, Spotify embeds are the most realistic starting point.

The system should not require Spotify login in V1.

Users should be able to paste an embed URL or link.

15. Entity: Goal
Purpose

Goals allow users to track reading intentions without making the app feel like a productivity tool.

Goals should be encouraging, not judgmental.

Suggested Fields
id
userId
goalType
title
description
targetValue
currentValue
startDate
endDate
status
relatedGenreId
relatedSeriesId
relatedFormat
createdAt
updatedAt
Goal Types
READ_BOOKS
READ_PAGES
LISTEN_HOURS
FINISH_TBR
FINISH_SERIES
READ_GENRE
REREAD_BOOKS
NO_BUY_UNTIL_TBR_UNDER
Goal Statuses
ACTIVE
COMPLETED
PAUSED
CANCELLED
16. Core Relationships
User Relationships
User one-to-many Collection
User one-to-many ReadingExperience
User one-to-many Quote
User one-to-many JournalEntry
User one-to-many MusicEntry
User one-to-many Goal
Book Relationships
Book many-to-many Author
Book many-to-many Genre
Series one-to-many Book
Book one-to-many ReadingExperience
Book one-to-many Quote
Book one-to-many JournalEntry
Book one-to-many MusicEntry
Book many-to-many Collection
ReadingExperience Relationships
ReadingExperience belongs to User
ReadingExperience belongs to Book
ReadingExperience one-to-many Quote
ReadingExperience one-to-many JournalEntry
ReadingExperience one-to-many MusicEntry
17. Text ERD Draft
User
 |
 |-- Collection
 |
 |-- Goal
 |
 |-- ReadingExperience
 |        |
 |        |-- Quote
 |        |-- JournalEntry
 |        |-- MusicEntry
 |
 |-- Quote
 |
 |-- JournalEntry
 |
 |-- MusicEntry


Book
 |
 |-- ReadingExperience
 |-- Quote
 |-- JournalEntry
 |-- MusicEntry
 |
 |-- Authors
 |-- Genres
 |-- Series


Collection
 |
 |-- Books


Series
 |
 |-- Books
 |-- MusicEntry
18. Analytics Requirements

The database must support flexible analytics.

Analytics should eventually answer:

Reading Analytics
How many books has the user completed?
How many are currently reading?
How many are on TBR?
How many were DNF'd?
How many rereads/relistens?
Genre Analytics
Most-read genre
Highest-rated genre
Lowest-rated genre
Most-DNF'd genre
Genre with most rereads
Author Analytics
Most-read author
Highest-rated author
Most-reread author
Author with most owned books
Format Analytics
Physical vs ebook vs audiobook
Highest-rated format
Most-completed format
Most-DNF'd format
Reading Experience Analytics
Average rating across rereads
Rating changes over time
Favorite reread format
Number of times each book was read/listened
TBR Analytics
TBR count
Oldest TBR
TBR by genre
TBR by format
Books bought vs books read later
Music Analytics
Books with music
Most common association type
Music attached to rereads
Series with playlists
19. Future-Proofing

The V1 database should leave room for:

Mobile App

Future mobile features may include:

Barcode scanning
Cover scanning
Quote photo capture
Quick progress updates
Recommendation Engine

Future recommendations may use:

ratings
genres
authors
rereads
DNF patterns
quotes
journal activity
music associations
TBR behavior
Shelf Customization

Future customization may require:

Theme
Decoration
ShelfLayout
Trinket
UserPreference
Family Libraries

Future family features may require:

Family
FamilyMember
ChildProfile
SharedCollection

These are not V1 requirements.

20. V1 Database Priorities

The V1 database should prioritize:

Authentication
Personal library ownership
Books
Authors
Genres
Series
Collections
Unlimited reading experiences
Quotes
Journal entries
Music entries
Goals
Analytics support

V1 should avoid:

Social feeds
Followers
Public reviews
Family accounts
Fan art uploads
Full shelf customization tables
Recommendation tables
21. Open Questions

These decisions need to be finalized before coding entities:

Book Ownership

Should V1 include a separate UserBookCopy entity?

This would be useful for tracking:

owned
borrowed
wishlist
library loan
special edition
audiobook copy
ebook copy

Possible decision:

For V1, keep ownership fields on Book or ReadingExperience.

For V1.5, introduce UserBookCopy if needed.

Genre Storage

Should genres be normalized as a table or stored as strings?

Recommendation:

Use Genre table for analytics.

Tags and Tropes

Should tags/tropes be in V1?

Recommendation:

Not required for first backend build, but database should allow adding them later.

Reading Progress Logs

Should every progress update be stored?

This would support detailed analytics about pauses and reading pace.

Recommendation:

Not required for initial V1, but likely valuable in V1.5.

Series

Should Series be required?

No.

A book may or may not belong to a series.

Music

Should music be tied to only Book in V1 or also ReadingExperience?

Recommendation:

Allow MusicEntry to optionally connect to Book, Series, and ReadingExperience from the beginning.

22. Initial Entity Build Order

Recommended backend entity build order:

User
Book
Author
Genre
Series
Collection
ReadingExperience
Quote
JournalEntry
MusicEntry
Goal

Reason:

Start with ownership and core book structure, then add personal content and analytics support.

23. Final Database North Star

The database should support this product truth:

Library Lane is not a place where a user stores book records.

Library Lane is a place where a user preserves their relationship with stories.

That means the database must preserve:

the story
the user’s copy/relationship
every reading experience
every thought
every quote
every song
every goal
every pattern discovered over time

# Library Lane Database Planning

## Document Purpose

This document defines the planned database architecture for the portfolio version of **Library Lane**.

Library Lane is not a simple CRUD book tracker. It is a personal library, reading journal, music-connected reading archive, analytics platform, and future mobile-ready application.

Because of that, the database must be designed carefully from the beginning.

The purpose of this document is to define:

- the database philosophy
- the core entities
- the relationships between entities
- the fields needed for Version 1
- future-proofing considerations
- analytics requirements
- mobile-readiness requirements
- open questions before implementation

This document should be treated as a living document. It will be updated as the product grows.

---

# 1. Database Philosophy

Library Lane’s database should support the product vision:

> Library Lane is not where users manage books.  
> Library Lane is where users build, remember, and enjoy their personal reading world.

The database should preserve more than basic book information.

It should preserve:

- what books the user owns or tracks
- what format they read in
- how many times they read or listened to a book
- how their thoughts changed across rereads
- what quotes mattered to them
- what music they associated with a story
- what collections they created
- what goals they set
- what patterns appear in their reading life

The database should be relational because Library Lane has many connected data types.

Examples:

- A user can have many books.
- A book can have many reading experiences.
- A book can belong to many collections.
- A book can have many quotes.
- A reading experience can have its own rating, notes, music, and progress.
- A series can contain multiple books.
- Genres and tags can support analytics and future recommendations.

For this reason, **PostgreSQL** is the correct database choice for Library Lane.

---

# 2. Core Database Rule

## One Book, Unlimited Reading Experiences

This is one of the most important architectural decisions in Library Lane.

A book should exist once in the user's library, but that book can have unlimited reading experiences attached to it.

A user may read the same favorite book:

- once physically
- once as an ebook
- once as an audiobook
- again years later
- again for a seasonal reread
- again after a new book in the series releases

Each of those experiences may have different:

- start dates
- finish dates
- formats
- progress
- ratings
- thoughts
- quotes
- notes
- music associations
- DNF reasons
- reread observations

Library Lane should never place a limit on how many reading experiences a user can document for a book.

A favorite book may have 1 reading experience.

Another may have 20.

Both should be fully supported.

---

## Why This Matters

Most book tracking apps treat a book as having one final status and one final rating.

Library Lane should do something richer.

A book can be:

- one shelf item visually
- one main book record
- one personal book page
- but many separate experiences underneath it

This allows the user to preserve the full history of their relationship with that book.

Example:

```text
Book: Fourth Wing

Reading Experience #1
- Format: Physical
- Status: Completed
- Rating: 4.5
- Notes: First read. Loved the worldbuilding.

Reading Experience #2
- Format: Audiobook
- Status: Completed
- Rating: 5
- Notes: Narration made the story even better.

Reading Experience #3
- Format: Ebook
- Status: Currently Reading
- Rating: Not rated yet
- Notes: Rereading before the next book releases.

3. V1 Database Scope

Version 1 should include the database structure needed to support the first strong portfolio version of Library Lane.

V1 should support:

user accounts
one personal library per user
books
authors
genres
series
collections/shelves
reading statuses
physical/ebook/audiobook formats
unlimited reading experiences
reading progress
quotes
journal entries
music entries
reading goals
flexible analytics

V1 should not yet require:

family libraries
public sharing
social feeds
recommendations
fan art uploads
full shelf decoration system
mobile-only scan history
AI recommendation tables

Those features should be designed for later versions.

4. Planned V1 Entities

The recommended Version 1 entities are:

User
Library
Book
Author
Genre
Series
Collection
ReadingExperience
Quote
JournalEntry
MusicEntry
Goal

Possible V1.5 or future entities:

Tag
ReadingProgressLog
UserPreference
Decoration
Theme
Recommendation
FanArt
FamilyMember
SharedLibrary
5. Entity: User
Purpose

The User entity represents the person using Library Lane.

Each user has their own private library.

For Version 1:

One User = One Personal Library

Family libraries, shared libraries, and child profiles are future features.

Suggested Fields
id
email
username
passwordHash
displayName
role
createdAt
updatedAt
lastLoginAt
Field Details
id

Primary key.

Recommended type:

Long

or eventually:

UUID

For simplicity, V1 can use Long.

email

User's login email.

Should be:

required
unique
lowercase normalized
validated as email format
username

Optional or required depending on final auth design.

Could be used for display or future public sharing.

For V1, email can be the main login credential.

passwordHash

Stores hashed password only.

Never store plain text passwords.

Spring Security should handle password hashing.

displayName

User-facing name.

Example:

Heather
Heather's Library

This can be used to personalize the library.

role

Possible values:

USER
ADMIN

V1 may only use USER, but including role support prepares for future admin tools.

createdAt / updatedAt

Audit fields for tracking record creation and updates.

lastLoginAt

Optional.

May be useful later for user engagement or security.

Relationships
User 1 -> 1 Library
User 1 -> many Books
User 1 -> many Collections
User 1 -> many ReadingExperiences
User 1 -> many Quotes
User 1 -> many JournalEntries
User 1 -> many MusicEntries
User 1 -> many Goals
6. Entity: Library
Purpose

The Library entity represents the user's personal digital library space.

Even though V1 has one library per user, having a Library entity gives us flexibility later.

Future possibilities:

multiple libraries per user
family library
public showcase library
themed library rooms
child shelves
shared household library
Suggested Fields
id
userId
name
description
selectedTheme
createdAt
updatedAt
Field Details
name

Default examples:

My Library
Heather's Library
description

Optional.

Could allow the user to describe their reading space.

selectedTheme

For V1, the theme may be hardcoded in the frontend.

Still, this field prepares for future theme selection.

Example:

WOODLAND_STORYBOOK
Relationships
Library 1 -> 1 User
Library 1 -> many Books
Library 1 -> many Collections
Library 1 -> many Goals
7. Entity: Book
Purpose

The Book entity represents a book/story in the user's library.

Important:

In Library Lane V1, a Book is user-owned/user-tracked data, not a global shared catalog record.

This means if two users add the same book, each user may have their own Book record.

This is simpler for V1 and allows more user customization.

Future versions could introduce a global catalog layer, but V1 does not need that complexity.

Suggested Fields
id
userId
libraryId
title
subtitle
description
coverImageUrl
publicationYear
publisher
isbn10
isbn13
language
pageCount
audiobookLengthSeconds
openLibraryId
googleBooksId
personalNotes
favorite
createdAt
updatedAt
Field Details
title

Required.

The minimum required field for a book.

subtitle

Optional.

Useful for nonfiction and special editions.

description

Optional.

May be autofilled by book metadata APIs.

coverImageUrl

Stores the selected cover image URL.

This may come from:

Open Library
Google Books
future custom upload
future user-selected edition

V1 should use external URLs when possible.

publicationYear

Important for analytics.

Future analytics may answer:

What publication years do I read most?
What decades do I rate highest?
Do I prefer classics or newer releases?
publisher

Optional.

Useful for edition tracking and book collectors.

isbn10 / isbn13

Optional but useful.

Used for metadata search, edition matching, barcode scanning later, and external API lookups.

language

Optional.

Useful for future multilingual support and analytics.

pageCount

Total pages for physical/ebook tracking.

Can be null for audiobooks or unknown books.

audiobookLengthSeconds

Total audiobook length in seconds.

This allows better audiobook analytics.

Example:

10 hours, 30 minutes = 37,800 seconds

Storing seconds is easier than storing text like "10h 30m".

openLibraryId / googleBooksId

External API identifiers.

These allow future metadata refreshes or edition lookups.

personalNotes

Optional high-level personal note about the book overall.

More detailed thoughts belong in JournalEntry.

favorite

Boolean.

Marks a book as a favorite.

Relationships
Book many -> 1 User
Book many -> 1 Library
Book many -> many Authors
Book many -> many Genres
Book many -> 1 Series optional
Book 1 -> many ReadingExperiences
Book 1 -> many Quotes
Book 1 -> many JournalEntries
Book 1 -> many MusicEntries
Book many -> many Collections
8. Entity: Author
Purpose

The Author entity stores author information separately from books so analytics can be flexible.

This allows reports like:

most-read author
highest-rated author
most-owned author
most reread author
author with most DNF'd books
Suggested Fields
id
name
bio
externalId
createdAt
updatedAt
Field Details
name

Required.

Author name as displayed to the user.

bio

Optional.

Not required for V1.

externalId

Optional.

Could store an Open Library author ID or another metadata ID.

Relationships
Author many -> many Books

This requires a join table:

book_authors
book_id
author_id
9. Entity: Genre
Purpose

Genres support organization, filtering, and analytics.

Genres may come from:

external APIs
user input
normalized app-defined categories later

For V1, genres can be simple.

Suggested Fields
id
name
createdAt
updatedAt
Field Details
name

Examples:

Fantasy
Romance
Mystery
Thriller
Historical Fiction
Nonfiction
Poetry
Manga
Graphic Novel
Young Adult
Science Fiction
Horror
Relationships
Genre many -> many Books

Join table:

book_genres
book_id
genre_id
Future Consideration

External APIs often return inconsistent categories.

Example:

Juvenile Fiction
Fantasy
Dragons
Romance
Young Adult Fiction

Later, Library Lane may need genre normalization.

For V1, we can store genre names as-is and refine later.

10. Entity: Series
Purpose

The Series entity groups books that belong to the same series.

This supports:

series tracking
series completion analytics
next unread book recommendations later
series-level music
series-level collections
Suggested Fields
id
userId
name
description
totalBooks
status
createdAt
updatedAt
Field Details
name

Required.

Example:

Harry Potter
A Court of Thorns and Roses
The Empyrean
Percy Jackson and the Olympians
totalBooks

Optional.

Some series are ongoing or unknown.

status

Possible values:

ONGOING
COMPLETED
UNKNOWN

This is the publication status of the series, not the user's personal completion status.

User completion can be calculated from books and reading experiences.

Relationships
Series 1 -> many Books
Series 1 -> many MusicEntries

Book should store:

seriesId
seriesNumber
11. Entity: Collection
Purpose

Collections represent custom bookshelves.

A collection is not just a filter.

In Library Lane, collections should feel like curated shelves.

Examples:

2026 Reads
Summer TBR
Fantasy Favorites
Comfort Books
Books That Made Me Cry
Audiobooks I Loved
Rereads
Series to Finish
Fall Favorites
Suggested Fields
id
userId
libraryId
name
description
theme
sortOrder
createdAt
updatedAt
Field Details
name

Required.

description

Optional.

Allows the user to describe the shelf.

theme

Optional.

Future-facing.

Could eventually control shelf appearance.

Example:

AUTUMN
WOODLAND
FANTASY
COZY
sortOrder

Optional.

Allows collections themselves to be displayed in a custom order.

Relationships
Collection many -> 1 User
Collection many -> 1 Library
Collection many -> many Books

Join table:

collection_books
collection_id
book_id
position

The position field is important for future drag-and-drop manual ordering.

12. Entity: ReadingExperience
Purpose

The ReadingExperience entity represents one specific read, reread, listen, relisten, or DNF attempt.

This is one of the most important entities in Library Lane.

A book can have unlimited reading experiences.

Suggested Fields
id
userId
bookId
experienceNumber
experienceType
format
status
startDate
finishDate
currentPage
totalPages
currentListeningSeconds
totalListeningSeconds
listeningSpeed
percentComplete
rating
reviewTitle
reviewText
dnfReason
isCurrent
createdAt
updatedAt
Field Details
experienceNumber

Sequential number for the book.

Example:

1
2
3
4

This helps display:

Read #1
Listen #2
Reread #3
experienceType

Possible values:

FIRST_READ
REREAD
FIRST_LISTEN
RELISTEN
DNF_ATTEMPT
PARTIAL_READ

This may be calculated automatically or selected by the user.

format

Possible values:

PHYSICAL
EBOOK
AUDIOBOOK

Future:

MIXED

Mixed could support someone reading physically and listening to the audiobook at the same time.

status

Possible values:

TBR
CURRENTLY_READING
COMPLETED
DNF
PAUSED

For V1, UI may use:

TBR
Currently Reading
Completed
DNF / Set Aside

PAUSED may be V1.5, but including it in the enum early is acceptable.

startDate / finishDate

Optional.

Useful for analytics:

average completion time
books read by month
books read by season
fastest reads
longest reads
currentPage / totalPages

Used for physical and ebook progress.

totalPages may duplicate Book.pageCount, but storing it on the experience allows different editions or formats.

currentListeningSeconds / totalListeningSeconds

Used for audiobooks.

listeningSpeed

Used for audiobook tracking.

Examples:

1.0
1.25
1.5
1.75
2.0

Use decimal type.

percentComplete

Can be calculated from page/time progress, but may also be stored for convenience.

For ebooks, percent may be the only progress value available.

rating

Rating for this specific experience.

This is important because the same book may receive different ratings across rereads or different formats.

Recommended type:

BigDecimal

or:

Double

Allowed values:

0.0 to 5.0

Could support half-stars or quarter-stars later.

reviewTitle / reviewText

Optional review/thoughts for this specific reading experience.

More detailed notes may go in JournalEntry.

dnfReason

Optional.

Only relevant if status is DNF.

Possible future controlled options:

Not the right time
Lost interest
Writing style
Pacing
Characters
Narration
Mood mismatch
Content issue
Too long
Other
isCurrent

Boolean.

Marks the active/current reading experience for that book.

This helps the Open Book View know where to open.

Important:

A book may have many old experiences, but usually only one current active experience.

Relationships
ReadingExperience many -> 1 User
ReadingExperience many -> 1 Book
ReadingExperience 1 -> many Quotes
ReadingExperience 1 -> many JournalEntries
ReadingExperience 1 -> many MusicEntries
13. Entity: Quote
Purpose

The Quote entity stores favorite lines, passages, or memorable moments from a book.

Quotes may belong to:

a book overall
a specific reading experience
a page/chapter/timestamp
Suggested Fields
id
userId
bookId
readingExperienceId
quoteText
pageNumber
chapter
timestampSeconds
speaker
note
spoiler
favorite
createdAt
updatedAt
Field Details
quoteText

Required.

The saved quote.

Important copyright note:

Users are saving quotes privately. Public quote sharing should be handled carefully later.

pageNumber

Optional.

For physical/ebook quotes.

chapter

Optional.

Useful for all formats.

timestampSeconds

Optional.

For audiobook quotes.

speaker

Optional.

Could represent character or narrator.

note

User's personal reaction to the quote.

spoiler

Boolean.

Allows spoiler-safe UI later.

favorite

Boolean.

Allows favorite quote collections later.

Relationships
Quote many -> 1 User
Quote many -> 1 Book
Quote many -> 1 ReadingExperience optional
14. Entity: JournalEntry
Purpose

The JournalEntry entity stores the user's reflections, notes, theories, reactions, and reading memories.

Journal entries can be tied to:

a book
a reading experience
a page/chapter/timestamp
a series later
Suggested Fields
id
userId
bookId
readingExperienceId
title
body
entryType
pageNumber
chapter
timestampSeconds
spoiler
createdAt
updatedAt
Entry Types

Possible values:

GENERAL
REVIEW
CHAPTER_NOTE
THEORY
REREAD_OBSERVATION
CHARACTER_NOTE
TROPE_NOTE
EMOTIONAL_REACTION
FAVORITE_MOMENT
Field Details
title

Optional.

Example:

Thoughts after chapter 12
Reread notes
Theory about the ending
body

Required.

Main journal text.

spoiler

Boolean.

Useful for spoiler-safe views.

Relationships
JournalEntry many -> 1 User
JournalEntry many -> 1 Book
JournalEntry many -> 1 ReadingExperience optional
15. Entity: MusicEntry
Purpose

The MusicEntry entity stores songs, playlists, albums, or other music associated with a book, series, scene, character, mood, trope, or reading experience.

Music is a core part of Library Lane's emotional identity.

Music should not feel like a random attachment. It should feel like part of the story experience.

Suggested Fields
id
userId
bookId
seriesId
readingExperienceId
title
artist
playlistName
platform
musicType
embedUrl
externalUrl
associationType
associationDescription
notes
createdAt
updatedAt
Field Details
title

For a song, album, playlist, or music entry label.

artist

Optional.

Useful for songs or albums.

playlistName

Optional.

Useful if the entry is a playlist.

platform

Possible values:

SPOTIFY
APPLE_MUSIC
YOUTUBE
YOUTUBE_MUSIC
SOUNDCLOUD
OTHER

For V1, Spotify embeds are the likely first implementation.

musicType

Possible values:

SONG
PLAYLIST
ALBUM
ARTIST
OTHER
embedUrl

Used to render embedded music player inside Library Lane.

Example:

Spotify embed URL
externalUrl

Fallback link to open the music externally.

associationType

Possible values:

WHOLE_BOOK
SERIES
READING_EXPERIENCE
CHAPTER
SCENE
CHARACTER
RELATIONSHIP
MOOD
TROPE
READING_MEMORY
GENERAL_VIBE
associationDescription

Optional text field.

Example:

This song reminds me of the final battle.
notes

Additional user notes.

Relationships
MusicEntry many -> 1 User
MusicEntry many -> 1 Book optional
MusicEntry many -> 1 Series optional
MusicEntry many -> 1 ReadingExperience optional

At least one association should exist:

bookId
seriesId
readingExperienceId

The exact validation can be handled in service logic.

16. Entity: Goal
Purpose

The Goal entity stores user reading goals.

Goals should feel encouraging, not judgmental.

Goal examples:

Read 50 books this year
Finish 20 books from TBR
Listen to 100 audiobook hours
Read 10 fantasy books
Finish 3 series
No-buy until TBR is under 20
Suggested Fields
id
userId
libraryId
goalType
title
description
targetValue
currentValue
startDate
endDate
status
relatedGenreId
relatedSeriesId
relatedFormat
createdAt
updatedAt
Goal Types

Possible values:

READ_BOOKS
READ_PAGES
LISTEN_HOURS
FINISH_TBR
FINISH_SERIES
READ_GENRE
REREAD_BOOKS
NO_BUY_UNTIL_TBR_UNDER
CUSTOM
Status Values
ACTIVE
COMPLETED
PAUSED
CANCELLED
Field Details
targetValue

Numeric target.

Example:

50 books
100 hours
20 TBR books
currentValue

Can be calculated, but may also be stored for convenience.

For many goals, current progress should be automatically calculated from reading data.

relatedGenreId

Optional.

Used for genre-specific goals.

relatedSeriesId

Optional.

Used for series completion goals.

relatedFormat

Optional.

Used for audiobook/ebook/physical goals.

Relationships
Goal many -> 1 User
Goal many -> 1 Library
Goal many -> 1 Genre optional
Goal many -> 1 Series optional
17. Optional Future Entity: Tag
Purpose

Tags support more flexible organization than genres.

Genres are broad.

Tags/tropes can be specific.

Examples:

found family
enemies to lovers
dragons
dark academia
magical school
slow burn
chosen one
cozy mystery
second chance romance

Tags will be especially useful for future recommendations.

Suggested Fields
id
userId
name
createdAt
updatedAt
Relationships
Tag many -> many Books

Join table:

book_tags
book_id
tag_id
Version Placement

Tags can be V1.5 if V1 becomes too large.

However, if analytics flexibility is a major priority, basic tag support may be worth including earlier.

18. Optional Future Entity: ReadingProgressLog
Purpose

The ReadingProgressLog entity records progress updates over time.

This would allow Library Lane to show:

when the user made progress
how quickly they read different parts
pauses and resumed reading
progress jumps
reading pace
how long a book took across different sessions

This directly supports the vision of seeing that a user read part of a book, put it down, came back later, and finished the rest quickly.

Suggested Fields
id
userId
readingExperienceId
progressType
pageNumber
listeningSeconds
percentComplete
note
loggedAt
Version Placement

This is very useful, but may be V1.5 rather than V1.

If included in V1, it should be automatic whenever progress is updated.

19. Enum Planning

The database and backend should use enums for controlled values.

Book Format
PHYSICAL
EBOOK
AUDIOBOOK
MIXED

V1 may only use:

PHYSICAL
EBOOK
AUDIOBOOK
Reading Status
TBR
CURRENTLY_READING
COMPLETED
DNF
PAUSED
Reading Experience Type
FIRST_READ
REREAD
FIRST_LISTEN
RELISTEN
DNF_ATTEMPT
PARTIAL_READ
Ownership Status

Potential future enum:

OWNED
BORROWED
LIBRARY_LOAN
WISHLIST
NOT_OWNED
PREORDER
GIFTED

Ownership may be added directly to Book or a future UserBookCopy entity.

Music Platform
SPOTIFY
APPLE_MUSIC
YOUTUBE
YOUTUBE_MUSIC
SOUNDCLOUD
OTHER
Music Type
SONG
PLAYLIST
ALBUM
ARTIST
OTHER
Music Association Type
WHOLE_BOOK
SERIES
READING_EXPERIENCE
CHAPTER
SCENE
CHARACTER
RELATIONSHIP
MOOD
TROPE
READING_MEMORY
GENERAL_VIBE
Goal Type
READ_BOOKS
READ_PAGES
LISTEN_HOURS
FINISH_TBR
FINISH_SERIES
READ_GENRE
REREAD_BOOKS
NO_BUY_UNTIL_TBR_UNDER
CUSTOM
Goal Status
ACTIVE
COMPLETED
PAUSED
CANCELLED
Journal Entry Type
GENERAL
REVIEW
CHAPTER_NOTE
THEORY
REREAD_OBSERVATION
CHARACTER_NOTE
TROPE_NOTE
EMOTIONAL_REACTION
FAVORITE_MOMENT
20. Relationship Summary
User Relationships
User 1 -> 1 Library
User 1 -> many Books
User 1 -> many Collections
User 1 -> many ReadingExperiences
User 1 -> many Quotes
User 1 -> many JournalEntries
User 1 -> many MusicEntries
User 1 -> many Goals
Library Relationships
Library many -> 1 User
Library 1 -> many Books
Library 1 -> many Collections
Library 1 -> many Goals
Book Relationships
Book many -> 1 User
Book many -> 1 Library
Book many -> many Authors
Book many -> many Genres
Book many -> 1 Series optional
Book 1 -> many ReadingExperiences
Book 1 -> many Quotes
Book 1 -> many JournalEntries
Book 1 -> many MusicEntries
Book many -> many Collections
ReadingExperience Relationships
ReadingExperience many -> 1 User
ReadingExperience many -> 1 Book
ReadingExperience 1 -> many Quotes
ReadingExperience 1 -> many JournalEntries
ReadingExperience 1 -> many MusicEntries
Collection Relationships
Collection many -> 1 User
Collection many -> 1 Library
Collection many -> many Books
Series Relationships
Series many -> 1 User
Series 1 -> many Books
Series 1 -> many MusicEntries
21. ERD Text Draft

Initial V1 ERD concept:

User
 |
 |-- Library
 |     |
 |     |-- Collection
 |     |      |
 |     |      |-- Book
 |     |
 |     |-- Goal
 |
 |-- Book
 |     |
 |     |-- Author
 |     |-- Genre
 |     |-- Series
 |     |-- ReadingExperience
 |     |       |
 |     |       |-- Quote
 |     |       |-- JournalEntry
 |     |       |-- MusicEntry
 |     |
 |     |-- Quote
 |     |-- JournalEntry
 |     |-- MusicEntry
 |
 |-- Goal

More detailed:

users
  id PK

libraries
  id PK
  user_id FK

books
  id PK
  user_id FK
  library_id FK
  series_id FK nullable

authors
  id PK

book_authors
  book_id FK
  author_id FK

genres
  id PK

book_genres
  book_id FK
  genre_id FK

series
  id PK
  user_id FK

collections
  id PK
  user_id FK
  library_id FK

collection_books
  collection_id FK
  book_id FK
  position

reading_experiences
  id PK
  user_id FK
  book_id FK

quotes
  id PK
  user_id FK
  book_id FK
  reading_experience_id FK nullable

journal_entries
  id PK
  user_id FK
  book_id FK
  reading_experience_id FK nullable

music_entries
  id PK
  user_id FK
  book_id FK nullable
  series_id FK nullable
  reading_experience_id FK nullable

goals
  id PK
  user_id FK
  library_id FK
  related_genre_id FK nullable
  related_series_id FK nullable
22. Analytics Requirements

The database should support flexible and extensive analytics.

V1 and future analytics should be able to answer:

General Reading
How many books are in the library?
How many books are completed?
How many are currently being read?
How many are TBR?
How many were DNF?
How many books were reread?
How many total reading experiences exist?
Genre Analytics
Most-read genre
Highest-rated genre
Lowest-rated genre
Most-owned genre
Most-DNF'd genre
Genre most likely to be reread
Genre most likely to remain on TBR
Author Analytics
Most-read author
Highest-rated author
Most-reread author
Author with most completed books
Author with most DNF'd books
Author most often added to TBR
Format Analytics
Physical vs ebook vs audiobook counts
Format completed most often
Format rated highest
Format DNF'd most often
Format most likely to be reread/relistened
Audiobook hours listened
Average audiobook speed
Time Analytics
Books completed by month
Books completed by year
Best reading month
Best reading season
Average days to complete a book
Fastest completed book
Longest completed book
Longest pause before completion
Reread Analytics
Most reread books
Books with changing ratings over time
Average rating across experiences
Rating difference between first read and later rereads
Formats used across rereads
Notes/quotes added during each experience
TBR Analytics
Total TBR books
Oldest TBR book
Newest TBR book
TBR by genre
TBR by author
TBR by format
TBR vs completed ratio
Music Analytics

Potential future analytics:

Books with music attached
Genres most often associated with music
Songs linked to scenes/characters
Reading experiences with soundtracks
Goal Analytics
Active goals
Completed goals
Goal progress
No-buy goal progress
TBR reduction progress
Series completion goal progress
23. Open Book View Data Requirements

The database must support the Open Book View experience.

When a user clicks a book from the shelf, the frontend should be able to load:

book overview
cover
title
author
series
publication details
current reading experience
all previous reading experiences
quotes
journal entries
music entries
collections
book-level insights

The Open Book View should initially open to the user's most current reading experience.

The backend should support identifying the current experience through:

ReadingExperience.isCurrent = true

or service logic that finds the latest active experience.

24. Search Requirements

Search must be flexible and case-insensitive.

The capstone version had a known issue where searching required lowercase to find some books.

The portfolio version must fix this.

Search should support:

title
author
series
genre
collection
status
format
tags later

Expected behavior:

Harry Potter
harry potter
HARRY
potter
Rowling
fantasy

should all be able to return relevant results.

PostgreSQL search should use case-insensitive matching.

Possible approaches:

LOWER(field) LIKE LOWER(:query)
ILIKE in PostgreSQL native queries
full-text search later
25. Mobile-Readiness Requirements

The database must support a future React Native mobile app.

This means:

no web-only assumptions
clean IDs
predictable relationships
API-friendly structure
efficient queries
support for quick progress updates
support for barcode scanning later
support for cover scanning later
support for mobile quote capture later

Future mobile features may require additional fields, but V1 should not block them.

26. Future-Proofing Decisions
Recommendation Engine

Future recommendations will depend on:

ratings
genres
authors
completion status
DNF patterns
rereads
music associations
quotes
journal activity
TBR behavior

The current schema supports those signals.

Shelf Customization

Future shelf customization may require:

Decoration
Theme
ShelfLayout
BookPosition

For now, Collection and the collection_books.position field prepare for future ordering.

Fan Art / Visual Boards

Future fan art should not be part of V1 database.

Potential future entity:

VisualMedia

Fields might include:

id
userId
bookId
seriesId
imageUrl
sourceUrl
attribution
notes
createdAt

This should be handled carefully due to copyright and storage concerns.

Family Libraries

Future family support may require:

Household
FamilyMember
ChildProfile
SharedLibrary

Not V1.

27. Implementation Notes for Spring Boot

Recommended package structure:

com.librarylane
  config
  controllers
  dto
  entities
  enums
  exceptions
  repositories
  security
  services
Entity Naming

Use singular names:

User
Library
Book
Author
Genre
Series
Collection
ReadingExperience
Quote
JournalEntry
MusicEntry
Goal
Table Naming

Use plural or snake_case consistently.

Recommended table names:

users
libraries
books
authors
genres
series
collections
reading_experiences
quotes
journal_entries
music_entries
goals

Join tables:

book_authors
book_genres
collection_books
Audit Fields

Most entities should include:

createdAt
updatedAt

These can be handled with Hibernate annotations:

@CreationTimestamp
@UpdateTimestamp
Validation

Important backend validation:

title is required
email is required and unique
rating must be between 0 and 5
current page cannot exceed total pages
current listening seconds cannot exceed total listening seconds
finish date cannot be before start date
goal target must be positive
music URL must be valid if provided
28. V1 Implementation Priority

When coding begins, build entities in this order:

User
Library
Book
Author
Genre
Series
Collection
ReadingExperience
Quote
JournalEntry
MusicEntry
Goal

However, authentication may affect whether User is built first or after security setup.

29. Database Open Questions

These questions should be resolved before or during implementation:

Question 1

Should Book represent the user's personal book record only, or should there eventually be a separate global catalog table?

V1 decision:

Book is user-specific.

Future possible improvement:

GlobalBookMetadata + UserBook
Question 2

Should ownership status be stored on Book or a separate UserBookCopy entity?

V1 likely decision:

Store basic ownership fields on Book or ReadingExperience.

Future possible improvement:

UserBookCopy

This may become important if the user owns multiple editions of the same book.

Question 3

Should a user be able to own multiple copies/formats of the same book at once?

Long-term answer:

Yes.

V1 may simplify this by allowing ReadingExperience format to vary, even if the main Book record is singular.

Future enhancement:

UserBookCopy entity
Question 4

Should tags/tropes be V1 or V1.5?

Reason to include in V1:

analytics flexibility
future recommendations
better organization

Reason to delay:

scope control

Tentative decision:

Genres in V1.
Tags/Tropes in V1.5 unless time allows.
Question 5

Should ReadingProgressLog be V1 or V1.5?

Reason to include in V1:

supports detailed progress history
supports reading pace analytics

Reason to delay:

more complexity

Tentative decision:

V1.5 unless progress analytics become a V1 priority.
30. Final V1 Database Direction

The V1 database should support:

One user
One personal library
Many books
Unlimited reading experiences per book
Flexible collections
Quotes
Journal entries
Music entries
Goals
Extensive analytics
Future mobile expansion

The most important structural decision is:

Book 1 -> Many ReadingExperiences

This must be preserved throughout the application.

Library Lane should allow a reader to return to the same story again and again, documenting each experience without losing the history of previous reads.

That is one of the core features that makes Library Lane unique.

Closing Database North Star

The database should not merely store books.

It should preserve a reader's relationship with stories over time.

Every table, relationship, and field should support that larger purpose.