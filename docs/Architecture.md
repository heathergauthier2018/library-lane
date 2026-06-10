# Library Lane Architecture

Frontend

React
TypeScript

Backend

Spring Boot

Database

PostgreSQL

Deployment

Frontend:
Vercel

Backend:
Render

Database:
Render PostgreSQL

## Future Mobile Architecture

React Native

Shared backend

Shared PostgreSQL database

Shared authentication

# Library Lane Architecture

## Document Purpose

This document defines the planned architecture for the portfolio version of Library Lane.

Library Lane is a full-stack personal library, reading journal, music-connected reading archive, analytics platform, and future mobile-ready application.

This architecture document explains:

- the overall system design
- frontend architecture
- backend architecture
- database architecture
- authentication architecture
- API architecture
- future mobile architecture
- deployment architecture
- testing architecture
- future expansion strategy

This document is a living document and should be updated as the application evolves.

---

# 1. Architecture Philosophy

Library Lane should be built as a real software product, not as a one-time class project.

The architecture should support:

- long-term maintainability
- clean separation of concerns
- secure user accounts
- future mobile app support
- flexible analytics
- future recommendations
- future customization
- future scalability

The application should be designed around this core idea:

> One backend API should support both the web app and the future mobile app.

This means the backend must be clean, stable, and independent from any one frontend.

---

# 2. High-Level System Architecture

Library Lane will use a modern full-stack architecture:

```text
React + TypeScript Web App
        |
        | REST API
        v
Spring Boot Backend
        |
        | JPA / Hibernate
        v
PostgreSQL Database

Future mobile architecture:

React Native / Expo Mobile App
        |
        | Same REST API
        v
Spring Boot Backend
        |
        v
PostgreSQL Database

The backend is the source of truth.

The frontend and future mobile app are clients that consume the backend API.

3. Monorepo Structure

Library Lane uses a monorepo structure:

library-lane/
│
├── backend/
│   └── Spring Boot backend API
│
├── frontend/
│   └── React + TypeScript web app
│
├── docs/
│   ├── Product-Vision-Design-Bible.md
│   ├── Roadmap.md
│   ├── Database-Planning.md
│   ├── API-Planning.md
│   ├── Architecture.md
│   └── Tech-Stack-Decisions.md
│
├── assets/
│   ├── mockups/
│   ├── wireframes/
│   └── screenshots/
│
├── README.md
└── .gitignore
Why a Monorepo?

A monorepo is useful for Library Lane because:

frontend and backend are part of one product
documentation lives beside the code
project setup is easier to understand
portfolio visitors can see the whole system in one place
future mobile code can also be added later if desired

Possible future structure:

library-lane/
│
├── backend/
├── frontend/
├── mobile/
├── docs/
└── assets/
4. Backend Architecture
Backend Technology

The backend will use:

Java 21
Spring Boot 3.5.x
Maven
Spring Web
Spring Security
Spring Data JPA
Hibernate
PostgreSQL Driver
Validation
Lombok
Spring Boot DevTools
Backend Responsibility

The backend is responsible for:

authentication
authorization
user data protection
book data persistence
reading experience logic
collection logic
journal logic
quote logic
music entry logic
goal logic
analytics calculations
external book metadata API integration
future mobile support

The backend should not contain frontend-specific UI logic.

5. Backend Package Structure

Recommended package structure:

com.librarylane
│
├── config
├── controllers
├── dto
├── entities
├── enums
├── exceptions
├── repositories
├── security
└── services
config

Contains application configuration.

Possible classes:

CorsConfig
SecurityConfig
ModelMapperConfig later
controllers

Contains REST API controllers.

Possible controllers:

AuthController
BookController
CollectionController
ReadingExperienceController
QuoteController
JournalEntryController
MusicEntryController
GoalController
InsightController
BookSearchController
dto

Contains request and response objects.

DTOs prevent exposing database entities directly through APIs.

Possible DTOs:

CreateBookRequest
UpdateBookRequest
BookResponse
ReadingExperienceResponse
CreateReadingExperienceRequest
QuoteResponse
MusicEntryResponse
GoalResponse
InsightOverviewResponse
entities

Contains JPA entities.

Possible entities:

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
enums

Contains controlled values.

Possible enums:

BookFormat
ReadingStatus
ReadingExperienceType
MusicPlatform
MusicType
MusicAssociationType
GoalType
GoalStatus
JournalEntryType
SeriesStatus
UserRole
exceptions

Contains custom exceptions and global error handling.

Possible classes:

ResourceNotFoundException
UnauthorizedAccessException
DuplicateResourceException
GlobalExceptionHandler
repositories

Contains Spring Data JPA repositories.

Possible repositories:

UserRepository
LibraryRepository
BookRepository
AuthorRepository
GenreRepository
SeriesRepository
CollectionRepository
ReadingExperienceRepository
QuoteRepository
JournalEntryRepository
MusicEntryRepository
GoalRepository
security

Contains authentication and authorization logic.

Possible classes:

JwtService
JwtAuthenticationFilter
UserDetailsServiceImpl
PasswordEncoderConfig
services

Contains business logic.

Possible services:

AuthService
UserService
LibraryService
BookService
BookSearchService
CollectionService
ReadingExperienceService
QuoteService
JournalEntryService
MusicEntryService
GoalService
InsightService
6. Backend Layering Pattern

Library Lane should use a layered backend architecture.

Controller
   ↓
Service
   ↓
Repository
   ↓
Database
Controller Layer

Responsible for:

receiving HTTP requests
validating request bodies
calling services
returning responses

Controllers should not contain heavy business logic.

Service Layer

Responsible for:

business rules
ownership checks
validation beyond field validation
calculations
coordinating repositories
analytics logic

Most application logic should live here.

Repository Layer

Responsible for:

database access
JPA queries
custom search methods
persistence

Repositories should not contain business rules.

Entity Layer

Responsible for:

representing database tables
defining relationships
mapping fields

Entities should not be used directly as API responses.

DTO Layer

Responsible for:

request shapes
response shapes
protecting entity structure
making APIs stable
7. Database Architecture

Library Lane uses PostgreSQL because the data is highly relational.

Core relationships include:

User -> Library
User -> Books
Book -> ReadingExperiences
Book -> Quotes
Book -> JournalEntries
Book -> MusicEntries
Book -> Collections
Book -> Authors
Book -> Genres
Book -> Series

The most important database rule is:

One book can have unlimited reading experiences.

This supports rereads, relistens, DNF attempts, and changing reader opinions over time.

Planned V1 Entities
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
Future Entities
Tag
ReadingProgressLog
Decoration
Theme
UserPreference
Recommendation
FanArt
FamilyLibrary
8. Core Domain Architecture
User Domain

The user domain manages:

account creation
login
password hashing
user-specific data ownership
future profile settings

Core entity:

User

Related entities:

Library
Book
Collection
Goal
Library Domain

The library domain represents the user's personal reading world.

Core entity:

Library

The library contains:

books
collections
goals
future themes
future decorations

V1 rule:

One User = One Library

Future rule:

One User may have multiple libraries
Book Domain

The book domain manages book records and metadata.

Core entity:

Book

Related entities:

Author
Genre
Series
Collection
ReadingExperience
Quote
JournalEntry
MusicEntry
Reading Experience Domain

The reading experience domain is one of the most important parts of Library Lane.

Core entity:

ReadingExperience

It supports:

first reads
rereads
audiobook listens
relistens
DNF attempts
format changes
changing ratings
experience-specific notes
Journal Domain

The journal domain manages personal reader reflections.

Core entity:

JournalEntry

It supports:

reviews
theories
chapter notes
reread observations
favorite moments
character notes
emotional reactions
Quote Domain

The quote domain manages saved quotes.

Core entity:

Quote

Quotes can be tied to:

a book
a reading experience
a page
a chapter
an audiobook timestamp
Music Domain

The music domain manages songs and playlists connected to stories.

Core entity:

MusicEntry

Music can be associated with:

whole book
series
reading experience
chapter
scene
character
mood
trope
reading memory
Goal Domain

The goal domain manages reading goals.

Core entity:

Goal

Goals can include:

read X books
read X pages
listen to X audiobook hours
finish X TBR books
finish X series
no-buy until TBR is under X
Insight Domain

The insight domain calculates analytics.

Core service:

InsightService

The insight domain may not need its own table in V1.

It can calculate reports from existing data.

9. API Architecture

Library Lane will use REST APIs.

Base pattern:

/api/{resource}

Examples:

/api/auth
/api/books
/api/collections
/api/reading-experiences
/api/quotes
/api/journal
/api/music
/api/goals
/api/insights
/api/book-search
API Design Principles

APIs should be:

predictable
RESTful
JSON-based
mobile-friendly
secure
user-scoped
versionable later
User-Scoped Data

Every request that accesses personal data should be scoped to the authenticated user.

A user should never be able to access another user's books, notes, quotes, music, or goals.

Service layer methods should check ownership.

10. Authentication Architecture

V1 should include authentication.

Recommended approach:

Spring Security + JWT
Auth Flow
User registers
   ↓
Password is hashed
   ↓
User is saved
   ↓
User logs in
   ↓
Backend returns JWT
   ↓
Frontend stores token securely
   ↓
Frontend sends token with API requests
   ↓
Backend validates token
   ↓
Request proceeds
Auth Endpoints

Planned endpoints:

POST /api/auth/register
POST /api/auth/login
GET /api/auth/me
POST /api/auth/logout later
Password Security

Passwords must be hashed.

Recommended:

BCryptPasswordEncoder

Never store plain text passwords.

JWT Notes

JWT should include:

user id
email
role
expiration

JWT secret should be stored in environment variables.

Never commit JWT secrets to GitHub.

11. Authorization Architecture

Authorization ensures users can only access their own data.

Example:

A user can access:

/api/books/12

only if book 12 belongs to that user.

This should be enforced in the service layer.

Example rule:

Book.user.id must equal authenticatedUser.id

This applies to:

books
collections
reading experiences
quotes
journal entries
music entries
goals
12. DTO Architecture

Library Lane should not expose entities directly.

Instead:

Entity -> Response DTO
Request DTO -> Entity
Why Use DTOs?

DTOs help with:

security
clean API responses
avoiding circular references
controlling returned fields
future API stability
frontend-friendly data shapes
Example DTOs
BookResponse
BookSummaryResponse
CreateBookRequest
UpdateBookRequest
ReadingExperienceResponse
CreateReadingExperienceRequest
QuoteResponse
CreateQuoteRequest
MusicEntryResponse
CreateMusicEntryRequest
GoalResponse
CreateGoalRequest
Summary vs Detail DTOs

Use different DTOs for different views.

Example:

BookSummaryResponse

Used for bookshelf/list views.

Contains:

id
title
coverImageUrl
authors
status
currentProgress
averageRating
BookDetailResponse

Used for Open Book View.

Contains:

id
title
authors
genres
series
readingExperiences
quotes
journalEntries
musicEntries
collections
insights

This avoids overloading the frontend with unnecessary data.

13. Open Book View Architecture

The Open Book View is a major product feature.

When a user clicks a book from the shelf:

Shelf Book
   ↓
Book opening animation
   ↓
Open Book View

The backend should support loading enough data for this view.

Required Open Book Data

The frontend may need:

Book details
Authors
Genres
Series
Current reading experience
All reading experiences
Quotes
Journal entries
Music entries
Collections
Book-level insights
Possible Endpoint
GET /api/books/{id}

This could return a full BookDetailResponse.

Alternatively:

GET /api/books/{id}/overview
GET /api/books/{id}/experiences
GET /api/books/{id}/quotes
GET /api/books/{id}/music

For V1, a full detail endpoint is acceptable.

For performance later, split endpoints may be better.

14. Frontend Architecture
Frontend Technology

The frontend will use:

React
TypeScript
Vite
React Router
TanStack Query
React Hook Form
Zod
Framer Motion
Recharts
Frontend Responsibility

The frontend is responsible for:

user interface
interactive bookshelf
book opening animations
form handling
client-side validation
route protection
displaying API data
responsive layouts
accessible interactions

The frontend should not be responsible for core business rules.

Business rules belong in the backend.

15. Frontend Folder Architecture

Recommended frontend structure:

frontend/src/
│
├── api/
├── assets/
├── components/
├── features/
├── hooks/
├── layouts/
├── pages/
├── routes/
├── styles/
├── types/
└── utils/
Feature-Based Structure

As the app grows, organize by feature:

frontend/src/features/
│
├── auth/
├── library/
├── books/
├── collections/
├── readingExperiences/
├── journal/
├── quotes/
├── music/
├── goals/
└── insights/
Shared Components
frontend/src/components/
│
├── ui/
├── bookshelf/
├── book/
├── forms/
├── layout/
└── charts/
16. Frontend State Management

Recommended strategy:

Server State

Use:

TanStack Query

For:

books
collections
reading experiences
quotes
journal entries
music entries
goals
insights
Form State

Use:

React Hook Form

with:

Zod

For validation.

Local UI State

Use React state for:

modal open/closed
selected book animation
active tab/page in Open Book View
hover state
page turn state
Global State

Use context only for:

authentication
theme
maybe user preferences

Avoid unnecessary global state early.

17. Frontend Page Architecture

Planned pages:

Public Pages
LandingPage
LoginPage
RegisterPage
Authenticated Pages
MyLibraryPage
AddStoryPage or AddStoryModal
OpenBookViewPage
CollectionsPage
CollectionDetailPage
ReadingJournalPage
InsightsPage
GoalsPage
SettingsPage

Future pages:

SeriesPage
MusicLibraryPage
QuoteWallPage
ThemeStudioPage
Mobile-inspired QuickUpdatePage
18. Interactive Bookshelf Architecture

The bookshelf is a core UI system.

It should start as a 2.5D visual experience using:

React components
CSS transforms
Framer Motion

Avoid heavy 3D/WebGL in V1.

Core Components
BookshelfScene
Bookshelf
ShelfRow
ShelfBook
ShelfDecoration future
BookOpeningTransition
ShelfBook Data

A shelf book needs:

bookId
title
coverImageUrl
authors
status
currentProgress
Shelf Interaction

Desktop:

hover -> book lifts/slides slightly
click -> book opens

Keyboard:

focus -> visible focus state
Enter/Space -> open book

Mobile:

tap -> open book or preview
19. Animation Architecture

Use:

Framer Motion

for V1 animations.

Key animations:

book added to shelf
book hover/focus
book opening transition
page turn transition
modal transitions
insight card fade-ins
Reduced Motion

Animations must respect reduced motion preferences.

If reduced motion is enabled:

skip large transitions
reduce page turns
preserve functionality
20. Analytics Architecture

Analytics should initially be calculated from database queries and service logic.

Core service:

InsightService

Possible endpoints:

GET /api/insights/overview
GET /api/insights/genres
GET /api/insights/authors
GET /api/insights/formats
GET /api/insights/goals
GET /api/insights/tbr
V1 Analytics

V1 should support:

total books
completed books
TBR count
DNF count
average rating
most-read genre
most-read author
format breakdown
pages read
audiobook hours
goal progress
Future Analytics

Future analytics may require:

ReadingProgressLog
AnalyticsSnapshot
RecommendationScore

Not required for V1.

21. External API Architecture

Library Lane will use external book metadata APIs.

Planned options:

Open Library API
Google Books API
Backend-Based API Calls

The frontend should not directly depend on external book APIs.

Recommended flow:

Frontend searches book
   ↓
GET /api/book-search?query=fourth+wing
   ↓
Backend calls Open Library / Google Books
   ↓
Backend normalizes result
   ↓
Frontend displays suggestions
Why Backend-Based?

This allows:

consistent response format
API fallback logic
easier future provider changes
hiding API keys if needed
better validation
logging and caching later
22. Music Integration Architecture

V1 should use Spotify embeds and external links.

No full Spotify login is needed in V1.

V1 Flow
User pastes Spotify URL
   ↓
Frontend validates basic URL
   ↓
Backend stores URL
   ↓
Frontend renders embed
Music Storage

Music entries are stored in:

MusicEntry

Music can be associated with:

book
series
reading experience

Future:

Spotify API integration
playlist import
shareable soundtracks
23. Deployment Architecture

Planned deployment approach:

Frontend -> Vercel or Netlify
Backend -> Render / Railway / Fly.io
Database -> PostgreSQL provider

Final provider decisions should be made after reviewing current pricing/free-tier limitations.

Deployment Requirements
secure environment variables
database connection string not committed
JWT secret not committed
CORS configured correctly
frontend API URL configurable
backend health endpoint
demo account support
24. Environment Configuration

Backend environment variables may include:

DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
JWT_EXPIRATION
FRONTEND_ORIGIN
BOOK_API_PROVIDER

Frontend environment variables may include:

VITE_API_BASE_URL

Example files should be provided:

application-example.properties
.env.example

Never commit real secrets.

25. Testing Architecture
Backend Testing

Use:

JUnit
Spring Boot Test

Test areas:

services
validation
repositories
controllers
authentication
authorization
Frontend Testing

Use:

Playwright

Important E2E tests:

user can register
user can log in
user can add a book
search is case-insensitive
user can add reading experience
user can add quote
user can add music
user can create collection
user can view insights

Testing should become part of the portfolio value.

26. Security Architecture

Security priorities:

password hashing
JWT validation
protected routes
user ownership checks
environment variables
CORS restrictions
safe error messages
Protected Resources

These must be protected:

books
collections
reading experiences
quotes
journal entries
music entries
goals
insights
Public Resources

Possible public resources:

landing page
register
login
demo info
health check
27. Error Handling Architecture

Use a global exception handler.

Possible error format:

{
  "timestamp": "2026-06-08T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Book not found",
  "path": "/api/books/5"
}

Errors should be clear but should not expose sensitive internal details.

28. Performance Architecture

V1 performance considerations:

avoid loading all nested book data on every page
use summary DTOs for shelf view
use detail DTOs for Open Book View
paginate large libraries later
index searchable fields
avoid unnecessary API calls
use TanStack Query caching
Important Indexes Later

Potential database indexes:

books.user_id
books.title
reading_experiences.book_id
collections.user_id
quotes.book_id
journal_entries.book_id
music_entries.book_id

For better search later:

LOWER(title)
LOWER(author name)

or PostgreSQL full-text search.

29. Mobile-Ready Architecture

The future mobile app should reuse:

same backend
same database
same authentication
same book data
same reading experiences

Mobile should not require a separate backend.

Future mobile app:

React Native / Expo
        ↓
Spring Boot API
        ↓
PostgreSQL

Mobile-specific future features:

barcode scanning
cover scanning
quick progress update
quote capture
mobile widgets
Google Play deployment
30. Future Expansion Architecture
Recommendation Engine

Future recommendations can be built from:

ratings
genres
authors
rereads
DNFs
music associations
quotes
journal activity
TBR patterns

Possible future service:

RecommendationService

Possible future endpoints:

GET /api/recommendations/tbr
GET /api/recommendations/next-read
GET /api/recommendations/similar
Shelf Customization

Future customization may introduce:

Theme
Decoration
ShelfLayout
BookPosition
Family Library

Future family features may introduce:

Household
FamilyMember
ChildProfile
SharedLibrary

Not V1.

31. Development Workflow Architecture

Recommended workflow:

main branch = stable
feature branches = new work
pull requests = review before merge

Example branches:

feature/backend-entities
feature/auth
feature/book-search
feature/bookshelf-ui
feature/reading-experiences
feature/music
feature/insights

Even as a solo developer, using feature branches and pull requests creates a professional workflow.

32. Current Architecture Status

Current status:

Repository created
Documentation started
Backend generated
Frontend not yet generated
Database planning drafted
Architecture planning drafted

Next implementation steps:

1. Finalize backend package structure
2. Create enums
3. Create User entity
4. Create Library entity
5. Create Book entity
6. Create supporting entities
7. Configure PostgreSQL
8. Add authentication
9. Build frontend foundation
33. Architecture North Star

Library Lane architecture should always support this principle:

The app should preserve a reader’s relationship with stories over time.

This means the architecture must support:

repeated reading experiences
personal notes
ratings over time
quotes
music
collections
analytics
future recommendations
future mobile access

The architecture should be flexible enough to grow without losing the emotional core of the product.