# Library Lane Domain Model

## Document Purpose

This document defines the core business objects (domains) of Library Lane.

The Domain Model answers:

- What objects exist in the system?
- Why do they exist?
- What information belongs to them?
- What relationships do they have?
- What rules govern them?
- How do they support the Library Lane vision?

This document is intentionally focused on business concepts rather than technical implementation.

The goal is to understand the world of Library Lane before writing code.

---

# Core Product Philosophy

Library Lane is not primarily a database of books.

Library Lane is a system that preserves a reader's relationship with stories over time.

This distinction affects every domain decision.

Most reading apps treat a book as:

```text
Book
Status
Rating
Done

Library Lane treats a book as:

Book
↓
Many Reading Experiences
↓
Many Quotes
Many Journal Entries
Many Music Associations
Many Memories
Many Insights

A book is not an event.

A book is an ongoing relationship.

Domain Overview

Version 1 domains:

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

Future domains:

Tag
Trope
ProgressLog
Recommendation
Decoration
Theme
FanArt
FamilyLibrary
Achievement
ReadingWrapUp
Domain: User
Purpose

Represents the owner of a Library Lane account.

Every piece of data ultimately belongs to a user.

The User is the root of the ownership model.

Business Responsibilities

A User can:

register
log in
own a library
own books
own collections
own goals
own reading experiences
own journal entries
own quotes
own music entries

A User cannot access another user's data.

Core Fields
id
email
password
displayName
role
createdAt
updatedAt
Relationships
User
│
├── Library (1)
├── Books (many)
├── Collections (many)
├── Goals (many)
Important Rule

Version 1:

One User = One Library

Future versions may support multiple libraries.

Domain: Library
Purpose

Represents the user's personal reading world.

This is the conceptual home of everything.

The Library is not just storage.

The Library represents:

books
collections
goals
insights
future customization
Core Fields
id
name
description
createdAt
updatedAt
Relationships
Library
│
├── User
├── Books
├── Collections
├── Goals
Important Rule

Every User automatically receives a Library.

No User exists without a Library.

Domain: Book
Purpose

Represents a story in the user's library.

This is one of the most important domains.

A Book stores permanent story-level information.

It does NOT store temporary reading experience information.

What Belongs To A Book

Permanent information:

title
subtitle
description
authors
genres
series
publicationYear
publisher
isbn
coverImage
language
pageCount
audiobookLength
What Does NOT Belong To A Book

These belong to Reading Experiences:

current progress
rating
review
completion date
reread notes
listening speed

Because they change between experiences.

Core Fields
id
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
favorite
createdAt
updatedAt
Relationships
Book
│
├── Library
├── Authors
├── Genres
├── Series
├── Collections
├── ReadingExperiences
├── Quotes
├── JournalEntries
├── MusicEntries
Important Rule

A Book may have:

0
1
5
20
100

Reading Experiences.

There is no limit.

This is a core Library Lane principle.

Domain: Author
Purpose

Represents a book author.

Authors should be reusable.

Example

Bad:

Book
Author Name Text

Good:

Author
↓
Many Books
Core Fields
id
name
bio
website
photoUrl
Relationships
Author
│
└── Books
Important Rule

A Book may have multiple Authors.

An Author may have multiple Books.

Many-to-many relationship.

Domain: Genre
Purpose

Represents a reading category.

Genres support:

organization
filtering
insights
future recommendations
Core Fields
id
name
description
Examples
Fantasy
Romance
Science Fiction
Horror
Historical Fiction
Poetry
Biography
Relationships
Genre
│
└── Books

Many-to-many.

Domain: Series
Purpose

Represents a connected group of books.

Core Fields
id
name
description
totalBooks
status
Relationships
Series
│
├── Books
├── MusicEntries
Series Status

Uses:

ONGOING
COMPLETED
HIATUS
CANCELLED
Important Rule

Deleting a Series should never delete Books.

Only remove the relationship.

Domain: Collection
Purpose

Collections are the digital equivalent of custom bookshelves.

Users create them.

Examples
Fantasy Favorites
2026 Reading
Cozy Reads
Summer TBR
Books That Made Me Cry
Reread Shelf
Core Fields
id
name
description
theme
createdAt
updatedAt
Relationships
Collection
│
├── User
└── Books
Important Rule

A Book can belong to:

0
1
many

Collections.

Example

Fourth Wing:

Fantasy Favorites
2026 Reads
Dragon Books

all at the same time.

Domain: ReadingExperience
Purpose

Represents one reading event.

This is arguably the most important domain in Library Lane.

Examples

First Read:

Physical Book
January 2026
4.5 Stars

Reread:

Kindle
June 2026
5 Stars

Relisten:

Audiobook
August 2026
4 Stars

These are separate experiences.

Why This Domain Exists

Most apps overwrite reading history.

Library Lane preserves it.

Core Fields
id
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

reviewText

dnfReason

createdAt
updatedAt
Relationships
ReadingExperience
│
├── Book
├── Quotes
├── JournalEntries
├── MusicEntries
Important Rules
Unlimited Experiences

No maximum.

Experiences Never Overwrite

Every reread is preserved.

Ratings Are Experience-Specific

A user may rate:

First Read = 3 Stars

Second Read = 5 Stars

Both are valid.

Domain: Quote
Purpose

Stores meaningful passages.

Core Fields
id
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
Relationships
Quote
│
├── Book
└── ReadingExperience
Important Rule

A Quote can belong to:

Book only

or:

Book
+
ReadingExperience
Domain: JournalEntry
Purpose

Stores personal reading thoughts.

Examples
Review
Theory
Character Note
Reread Observation
Emotional Reaction
General Thought
Core Fields
id

title

body

entryType

pageNumber
chapter

timestampSeconds

spoiler

createdAt
updatedAt
Relationships
JournalEntry
│
├── Book
└── ReadingExperience
Important Rule

Journal entries should preserve the user's thinking over time.

Domain: MusicEntry
Purpose

Represents music connected to stories.

One of the most unique parts of Library Lane.

Examples
Song for a Character
Song for a Scene
Playlist for a Book
Album for a Series
Core Fields
id

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
Relationships
MusicEntry
│
├── Book
├── Series
└── ReadingExperience
Important Rule

Music can connect to:

Book
Series
Reading Experience

depending on user preference.

Supported Associations
BOOK
SERIES
READING_EXPERIENCE
CHAPTER
SCENE
CHARACTER
MOOD
TROPE
Domain: Goal
Purpose

Represents a reading objective.

Examples
Read 50 Books
Reduce TBR
Read More Fantasy
Finish 3 Series
Listen to 100 Hours
Core Fields
id

title

description

goalType

targetValue

currentValue

startDate

endDate

status

createdAt
updatedAt
Relationships
Goal
│
├── User
└── Library
Important Rule

Goals should be encouraging.

Not punitive.

Library Lane should feel welcoming.

Not like productivity software.

Cross-Domain Relationship Map
User
│
└── Library
      │
      ├── Books
      ├── Collections
      └── Goals

Book
│
├── Authors
├── Genres
├── Series
├── Collections
├── ReadingExperiences
├── Quotes
├── JournalEntries
└── MusicEntries

ReadingExperience
│
├── Quotes
├── JournalEntries
└── MusicEntries
V1 Domain Completion Checklist

The domain model is complete when:

User exists
Library exists
Book exists
Author exists
Genre exists
Series exists
Collection exists
ReadingExperience exists
Quote exists
JournalEntry exists
MusicEntry exists
Goal exists

All relationships are defined.

All ownership rules are defined.

All business rules are defined.

Domain Model North Star

Every domain decision should support this principle:

Preserve the reader's relationship with stories over time.

Not just books.

Not just ratings.

The full experience of reading.


---

### After you save this file

We are officially done with the major planning phase.

The next thing I would have you do is:

1. Create all backend packages in IntelliJ.
2. Create the `enums` package.
3. Create the 11 enum files.
4. Commit that work.
5. Then we start the first actual domain code:

```text
UserRole.java
User.java
Library.java