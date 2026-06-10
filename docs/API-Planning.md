# Library Lane API Planning

## V1 Endpoints

Authentication

POST /api/auth/register
POST /api/auth/login

Books

GET /api/books
GET /api/books/{id}
POST /api/books
PUT /api/books/{id}
DELETE /api/books/{id}

Collections

GET /api/collections
POST /api/collections

Reading Experiences

GET /api/books/{id}/experiences
POST /api/books/{id}/experiences

Quotes

GET /api/books/{id}/quotes
POST /api/books/{id}/quotes

Music

GET /api/books/{id}/music
POST /api/books/{id}/music

# Library Lane API Planning

## Document Purpose

This document defines the planned REST API architecture for the portfolio version of **Library Lane**.

Library Lane is designed as a full-stack application with:

- React + TypeScript web frontend
- Spring Boot backend
- PostgreSQL database
- future React Native mobile app

The API must support both the web app and the future mobile app.

This document defines:

- API philosophy
- base URL structure
- authentication endpoints
- book endpoints
- reading experience endpoints
- quote endpoints
- journal endpoints
- music endpoints
- collection endpoints
- goal endpoints
- insight endpoints
- search/autofill endpoints
- response structure
- error handling
- security rules
- future API expansion

This is a living document and should be updated as development progresses.

---

# 1. API Philosophy

Library Lane’s API should be:

- clear
- predictable
- secure
- mobile-ready
- user-scoped
- RESTful
- JSON-based
- easy for the frontend to consume
- flexible enough for future expansion

The API should not be designed only for the first React web app.

It should be designed as the stable backend contract for:

```text
React Web App
React Native Mobile App
Future Admin Tools
Future Integrations

The backend API is the source of truth for Library Lane data.

2. API Base Path

All backend API routes should begin with:

/api

Example:

/api/books
/api/collections
/api/music

This keeps API routes separate from any future static resources or server-rendered pages.

3. API Versioning

Versioning is not required immediately for V1.

Initial route format:

/api/books

Future route format if needed:

/api/v1/books

Recommended V1 decision:

Use /api for now.
Add /api/v1 later only if needed.
4. Authentication Strategy

Library Lane V1 should use:

Spring Security + JWT

Authentication must support:

web frontend
future mobile app
protected user data
private libraries
demo account access
5. Authentication Endpoints
Register
POST /api/auth/register
Purpose

Creates a new user account and personal library.

Request Body
{
  "email": "reader@example.com",
  "password": "SecurePassword123!",
  "displayName": "Heather"
}
Response Body
{
  "token": "jwt-token-here",
  "user": {
    "id": 1,
    "email": "reader@example.com",
    "displayName": "Heather",
    "role": "USER"
  }
}
Backend Actions
validate email
validate password
check email uniqueness
hash password
create user
create default library
generate JWT
return user summary
Login
POST /api/auth/login
Purpose

Authenticates a user and returns a JWT.

Request Body
{
  "email": "reader@example.com",
  "password": "SecurePassword123!"
}
Response Body
{
  "token": "jwt-token-here",
  "user": {
    "id": 1,
    "email": "reader@example.com",
    "displayName": "Heather",
    "role": "USER"
  }
}
Get Current User
GET /api/auth/me
Purpose

Returns the currently authenticated user.

Headers
Authorization: Bearer jwt-token-here
Response Body
{
  "id": 1,
  "email": "reader@example.com",
  "displayName": "Heather",
  "role": "USER",
  "libraryId": 1
}
Logout
POST /api/auth/logout
V1 Note

With JWT auth, logout can initially be handled on the frontend by removing the stored token.

A backend logout endpoint may be added later if refresh tokens or token blacklisting are implemented.

6. Authorization Rules

Every user-owned resource must be protected.

A user may only access their own:

library
books
collections
reading experiences
quotes
journal entries
music entries
goals
insights

Example rule:

Authenticated user ID must match resource owner user ID.

This should be enforced in the service layer.

7. Standard Response Patterns
Success Response

Most endpoints should return JSON.

Example:

{
  "id": 12,
  "title": "Fourth Wing"
}
List Response

Simple V1 list response:

[
  {
    "id": 1,
    "title": "Fourth Wing"
  },
  {
    "id": 2,
    "title": "Iron Flame"
  }
]

Future paginated response:

{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}

V1 can start simple, but large library support may require pagination later.

8. Standard Error Response

Use consistent error responses.

Example:

{
  "timestamp": "2026-06-08T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Book not found",
  "path": "/api/books/12"
}

Common statuses:

400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
500 Internal Server Error

Error messages should be clear but should not expose sensitive internal details.

9. Book API

Books are the center of Library Lane.

A book represents one story in the user’s library.

A book can have unlimited reading experiences.

Get All Books
GET /api/books
Purpose

Returns all books for the authenticated user.

Optional Query Parameters
search
status
format
genre
author
series
collectionId
favorite
sort

Example:

GET /api/books?search=harry&status=COMPLETED
Response Body
[
  {
    "id": 1,
    "title": "Fourth Wing",
    "coverImageUrl": "https://example.com/cover.jpg",
    "authors": ["Rebecca Yarros"],
    "seriesName": "The Empyrean",
    "currentStatus": "CURRENTLY_READING",
    "currentProgressPercent": 45,
    "averageRating": 4.75,
    "favorite": true
  }
]

This response should be lightweight enough for bookshelf and library views.

Get Book Detail
GET /api/books/{bookId}
Purpose

Returns full details for the Open Book View.

Response Body
{
  "id": 1,
  "title": "Fourth Wing",
  "subtitle": null,
  "description": "Book description...",
  "coverImageUrl": "https://example.com/cover.jpg",
  "publicationYear": 2023,
  "publisher": "Red Tower Books",
  "isbn10": null,
  "isbn13": "9781649374042",
  "language": "en",
  "pageCount": 528,
  "audiobookLengthSeconds": null,
  "favorite": true,
  "authors": [
    {
      "id": 1,
      "name": "Rebecca Yarros"
    }
  ],
  "genres": [
    {
      "id": 1,
      "name": "Fantasy"
    }
  ],
  "series": {
    "id": 1,
    "name": "The Empyrean",
    "seriesNumber": 1
  },
  "collections": [
    {
      "id": 2,
      "name": "Fantasy Favorites"
    }
  ],
  "currentReadingExperience": {
    "id": 10,
    "status": "CURRENTLY_READING",
    "format": "PHYSICAL",
    "percentComplete": 45
  },
  "readingExperiences": [],
  "quotes": [],
  "journalEntries": [],
  "musicEntries": []
}
Create Book
POST /api/books
Purpose

Adds a new book to the user’s library.

Request Body
{
  "title": "Fourth Wing",
  "subtitle": null,
  "description": "Book description...",
  "coverImageUrl": "https://example.com/cover.jpg",
  "publicationYear": 2023,
  "publisher": "Red Tower Books",
  "isbn10": null,
  "isbn13": "9781649374042",
  "language": "en",
  "pageCount": 528,
  "audiobookLengthSeconds": null,
  "authorNames": ["Rebecca Yarros"],
  "genreNames": ["Fantasy", "Romance"],
  "seriesName": "The Empyrean",
  "seriesNumber": 1,
  "collectionIds": [2, 3],
  "initialReadingExperience": {
    "format": "PHYSICAL",
    "status": "CURRENTLY_READING",
    "currentPage": 100,
    "totalPages": 528,
    "percentComplete": 18.9
  }
}
Backend Actions
validate required fields
create book
create or connect authors
create or connect genres
create or connect series
assign collections
create initial reading experience
return created book
Update Book
PUT /api/books/{bookId}
Purpose

Updates book-level metadata.

Request Body
{
  "title": "Fourth Wing",
  "publicationYear": 2023,
  "pageCount": 528,
  "favorite": true
}
Delete Book
DELETE /api/books/{bookId}
Purpose

Removes a book from the user’s library.

Important Note

Deleting a book should also remove or handle related:

reading experiences
quotes
journal entries
music entries
collection links

This should be a deliberate action with frontend confirmation.

10. Book Search / Metadata API

Book search helps users add books quickly.

The frontend should not call Open Library or Google Books directly.

Instead:

Frontend -> Library Lane Backend -> External Book APIs
Search External Books
GET /api/book-search?query=fourth+wing
Purpose

Searches external metadata providers.

Response Body
[
  {
    "externalId": "google-books-id",
    "source": "GOOGLE_BOOKS",
    "title": "Fourth Wing",
    "authors": ["Rebecca Yarros"],
    "coverImageUrl": "https://example.com/cover.jpg",
    "publicationYear": 2023,
    "pageCount": 528,
    "isbn13": "9781649374042",
    "description": "Book description..."
  }
]
Get External Book Detail
GET /api/book-search/{source}/{externalId}
Purpose

Fetches more detailed metadata for a selected search result.

11. Reading Experience API

A reading experience represents one read, reread, listen, relisten, or DNF attempt.

A book can have unlimited reading experiences.

Get Reading Experiences for Book
GET /api/books/{bookId}/reading-experiences
Response Body
[
  {
    "id": 1,
    "experienceNumber": 1,
    "experienceType": "FIRST_READ",
    "format": "PHYSICAL",
    "status": "COMPLETED",
    "startDate": "2026-01-01",
    "finishDate": "2026-01-10",
    "rating": 4.5,
    "percentComplete": 100
  },
  {
    "id": 2,
    "experienceNumber": 2,
    "experienceType": "REREAD",
    "format": "AUDIOBOOK",
    "status": "CURRENTLY_READING",
    "percentComplete": 60
  }
]
Create Reading Experience
POST /api/books/{bookId}/reading-experiences
Request Body
{
  "experienceType": "REREAD",
  "format": "AUDIOBOOK",
  "status": "CURRENTLY_READING",
  "startDate": "2026-06-08",
  "totalListeningSeconds": 37800,
  "currentListeningSeconds": 12000,
  "listeningSpeed": 1.5
}
Backend Actions
verify book belongs to authenticated user
calculate next experience number
create reading experience
optionally set as current
return created experience
Update Reading Experience
PUT /api/reading-experiences/{experienceId}
Request Body
{
  "status": "COMPLETED",
  "finishDate": "2026-06-15",
  "rating": 5,
  "reviewText": "Loved this even more on reread."
}
Update Progress
PATCH /api/reading-experiences/{experienceId}/progress
Physical/Ebook Request
{
  "currentPage": 250,
  "totalPages": 528
}
Audiobook Request
{
  "currentListeningSeconds": 18000,
  "totalListeningSeconds": 37800,
  "listeningSpeed": 1.5
}
Response Body
{
  "id": 2,
  "percentComplete": 47.6,
  "status": "CURRENTLY_READING"
}
Delete Reading Experience
DELETE /api/reading-experiences/{experienceId}
Purpose

Deletes one reading experience without deleting the book.

This should be protected by confirmation in the UI.

12. Quote API

Quotes can belong to a book and optionally a specific reading experience.

Get Quotes for Book
GET /api/books/{bookId}/quotes
Create Quote
POST /api/books/{bookId}/quotes
Request Body
{
  "readingExperienceId": 2,
  "quoteText": "Quote text here.",
  "pageNumber": 120,
  "chapter": "Chapter 10",
  "timestampSeconds": null,
  "speaker": "Character Name",
  "note": "This line stood out to me.",
  "spoiler": false,
  "favorite": true
}
Update Quote
PUT /api/quotes/{quoteId}
Delete Quote
DELETE /api/quotes/{quoteId}
13. Journal API

Journal entries store user thoughts, notes, theories, and reflections.

Get Journal Entries
GET /api/journal

Optional query parameters:

bookId
readingExperienceId
entryType
spoiler
Get Journal Entries for Book
GET /api/books/{bookId}/journal
Create Journal Entry
POST /api/books/{bookId}/journal
Request Body
{
  "readingExperienceId": 2,
  "title": "Reread thoughts",
  "body": "I noticed so much foreshadowing this time.",
  "entryType": "REREAD_OBSERVATION",
  "pageNumber": 250,
  "chapter": "Chapter 20",
  "timestampSeconds": null,
  "spoiler": true
}
Update Journal Entry
PUT /api/journal/{entryId}
Delete Journal Entry
DELETE /api/journal/{entryId}
14. Music API

Music entries allow users to connect songs and playlists to stories.

Music can connect to:

whole book
series
reading experience
chapter
scene
character
mood
trope
reading memory
Get Music for Book
GET /api/books/{bookId}/music
Get Music for Series
GET /api/series/{seriesId}/music
Create Music Entry
POST /api/music
Request Body
{
  "bookId": 1,
  "seriesId": null,
  "readingExperienceId": 2,
  "title": "Song Title",
  "artist": "Artist Name",
  "playlistName": null,
  "platform": "SPOTIFY",
  "musicType": "SONG",
  "embedUrl": "https://open.spotify.com/embed/track/example",
  "externalUrl": "https://open.spotify.com/track/example",
  "associationType": "SCENE",
  "associationDescription": "This song fits the final battle.",
  "notes": "The bridge reminds me of the ending."
}
Validation Rule

At least one of the following should be present:

bookId
seriesId
readingExperienceId
Update Music Entry
PUT /api/music/{musicEntryId}
Delete Music Entry
DELETE /api/music/{musicEntryId}
15. Collection API

Collections represent custom shelves.

A book can belong to multiple collections.

Get Collections
GET /api/collections
Response Body
[
  {
    "id": 1,
    "name": "Fantasy Favorites",
    "description": "My favorite fantasy reads.",
    "bookCount": 12
  }
]
Create Collection
POST /api/collections
Request Body
{
  "name": "Fantasy Favorites",
  "description": "My favorite fantasy reads.",
  "theme": "FANTASY"
}
Get Collection Detail
GET /api/collections/{collectionId}
Response Body
{
  "id": 1,
  "name": "Fantasy Favorites",
  "description": "My favorite fantasy reads.",
  "theme": "FANTASY",
  "books": []
}
Update Collection
PUT /api/collections/{collectionId}
Delete Collection
DELETE /api/collections/{collectionId}

Important:

Deleting a collection should not delete the books inside it.

It should only remove the collection and book associations.

Add Book to Collection
POST /api/collections/{collectionId}/books/{bookId}
Remove Book from Collection
DELETE /api/collections/{collectionId}/books/{bookId}
16. Series API

Series support may be included in V1 or added shortly after.

Get Series
GET /api/series
Create Series
POST /api/series
Request Body
{
  "name": "The Empyrean",
  "description": "Fantasy series.",
  "totalBooks": 5,
  "status": "ONGOING"
}
Get Series Detail
GET /api/series/{seriesId}
Response Body
{
  "id": 1,
  "name": "The Empyrean",
  "totalBooks": 5,
  "status": "ONGOING",
  "books": [],
  "musicEntries": []
}
Update Series
PUT /api/series/{seriesId}
Delete Series
DELETE /api/series/{seriesId}

Deleting a series should not delete books.

It should remove the series association from books.

17. Goal API

Goals help users track reading objectives.

Get Goals
GET /api/goals
Create Goal
POST /api/goals
Request Body
{
  "goalType": "READ_BOOKS",
  "title": "Read 50 books this year",
  "description": "My 2026 reading goal.",
  "targetValue": 50,
  "startDate": "2026-01-01",
  "endDate": "2026-12-31"
}
Update Goal
PUT /api/goals/{goalId}
Delete Goal
DELETE /api/goals/{goalId}
Get Goal Progress
GET /api/goals/{goalId}/progress
Response Body
{
  "goalId": 1,
  "targetValue": 50,
  "currentValue": 12,
  "percentComplete": 24,
  "status": "ACTIVE"
}
18. Insights API

Insights power analytics and reporting.

Overview Insights
GET /api/insights/overview
Response Body
{
  "totalBooks": 120,
  "completedBooks": 40,
  "currentlyReading": 3,
  "tbrBooks": 70,
  "dnfBooks": 7,
  "averageRating": 4.2,
  "totalPagesRead": 12500,
  "totalAudiobookHours": 84,
  "totalReadingExperiences": 55
}
Genre Insights
GET /api/insights/genres

Possible response:

[
  {
    "genre": "Fantasy",
    "booksCompleted": 20,
    "averageRating": 4.6,
    "dnfCount": 1
  }
]
Author Insights
GET /api/insights/authors
Format Insights
GET /api/insights/formats

Possible response:

[
  {
    "format": "AUDIOBOOK",
    "completedCount": 12,
    "averageRating": 4.4,
    "totalListeningHours": 84
  }
]
TBR Insights
GET /api/insights/tbr
Goal Insights
GET /api/insights/goals
19. Search API

Library search must be flexible and case-insensitive.

Search User Library
GET /api/search?query=harry
Search Across
book title
author name
series name
genre name
collection name
later tags/tropes
Expected Behavior

These should all work:

Harry Potter
harry potter
HARRY
potter
Rowling
fantasy
20. Health Check API

A health endpoint is useful for deployment.

GET /api/health
Response Body
{
  "status": "UP",
  "service": "Library Lane API"
}

This can be public.

21. CORS Requirements

The backend must allow requests from the frontend.

Development frontend origin:

http://localhost:5173

Production frontend origin:

https://library-lane.vercel.app

or final deployed URL.

CORS should be configured securely.

Do not allow all origins in production.

22. Environment Variables

Backend should use environment variables for:

DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
JWT_EXPIRATION
FRONTEND_ORIGIN

Frontend should use:

VITE_API_BASE_URL
23. API Security Summary

Protected endpoints:

/api/books/**
/api/collections/**
/api/reading-experiences/**
/api/quotes/**
/api/journal/**
/api/music/**
/api/goals/**
/api/insights/**
/api/search/**

Public endpoints:

/api/auth/register
/api/auth/login
/api/health

Possibly public later:

/api/demo
24. V1 Endpoint Priority

Build endpoints in this order:

Health check
Auth register/login/me
Books
Reading experiences
Collections
Quotes
Journal entries
Music entries
Goals
Insights
Book metadata search
Library search
25. Future API Expansion

Future endpoints may include:

Tags
GET /api/tags
POST /api/tags
POST /api/books/{bookId}/tags/{tagId}
Progress Logs
GET /api/reading-experiences/{id}/progress-log
Decorations
GET /api/decorations
POST /api/decorations
Recommendations
GET /api/recommendations/tbr
GET /api/recommendations/next-read
GET /api/recommendations/similar
Mobile Scan
POST /api/mobile/scan/isbn
POST /api/mobile/scan/cover
26. API North Star

The API should support the core Library Lane experience:

A user clicks a book on the shelf, opens it like a real story, and can explore everything connected to that book: progress, rereads, quotes, journal entries, music, collections, and insights.

Every endpoint should support that experience clearly, securely, and predictably.