# Library Lane Roadmap

Phase 1

Foundation

Repository setup
Documentation
Spring Boot setup
React setup
Database design
Authentication

Phase 2

Core Library

Books
Collections
Bookshelves
Reading status
Progress tracking

Phase 3

Open Book Experience

Book opening animation
Page turn animations
Reading experiences
Quotes
Music

Phase 4

Analytics

Genre reports
Author reports
Reading habits
Goals
Progress insights

Phase 5

Customization

Bookshelf themes
Trinkets
Decorations
Personal library spaces

Phase 6

Mobile App

React Native
Android
Google Play

# Library Lane Roadmap

## Document Purpose

This roadmap defines the planned development path for the portfolio version of **Library Lane**.

Library Lane is being built as a long-term software product, not a one-time class project. The roadmap exists to keep development organized, prevent scope creep, and make sure each phase supports the larger product vision.

The roadmap should answer:

- what gets built first
- what gets delayed
- what belongs in Version 1
- what belongs in later versions
- what technical foundation must exist before advanced features
- how the web app leads into the future mobile app
- how each phase supports the portfolio value of the project

This is a living document and should be updated as the project evolves.

---

# 1. Roadmap Philosophy

Library Lane should be built in intentional layers.

The goal is not to rush every dream feature into the first version.

The goal is to build a strong foundation that can grow.

Each phase should have a clear purpose:

```text
Foundation
Core Product
Immersive Experience
Personalization
Analytics
Polish
Deployment
Mobile
Future Intelligence

The roadmap should protect the product from becoming chaotic.

A feature may be exciting, but that does not mean it belongs in Version 1.

2. Product North Star

Every roadmap decision should return to this principle:

Library Lane is not where users manage books.
Library Lane is where users build, remember, and enjoy their personal reading world.

A feature should move forward if it supports:

personal library building
reading tracking
emotional connection to books
customization
journaling
analytics
future mobile readiness

A feature should be delayed if it creates too much complexity before the foundation is stable.

3. Version Overview
V1 — Portfolio Web App

The first major version.

Purpose:

Prove Library Lane’s core identity as a cozy, immersive personal library and reading journal web application.

V1 should be strong enough to show recruiters, include in a portfolio, and use as the base for future mobile development.

V1.5 — Polish and Personalization

Purpose:

Improve visual quality, customization, animations, and analytics after V1 works.

V1.5 should make the app feel more magical and personal.

V2 — Mobile Companion App

Purpose:

Bring Library Lane to mobile using the same backend and database.

V2 should focus on convenience: quick updates, adding books, notes, quotes, and future scanning.

V3 — Personalized Intelligence

Purpose:

Add personalized recommendations and deeper reading intelligence.

Recommendations should be based on the user's actual reading life, not generic popularity.

Future Versions

Purpose:

Explore family libraries, advanced customization, optional sharing, fan art boards, reading recaps, and other dream features.

4. Phase 0 — Product Foundation
Status

In progress / mostly complete.

Purpose

Before writing major code, Library Lane needs a clear foundation.

This phase establishes:

product vision
technical architecture
database planning
API planning
roadmap
GitHub repository
project structure
Tasks
Create GitHub repository.
Create monorepo folder structure.
Create backend folder.
Create frontend folder.
Create docs folder.
Create assets folder.
Create Product Vision & Design Bible.
Create Tech Stack Decisions document.
Create Database Planning document.
Create Architecture document.
Create API Planning document.
Create Roadmap document.
Install Java 21.
Generate Spring Boot backend.
Commit project foundation to GitHub.
Success Criteria

Phase 0 is complete when:

the GitHub repository exists
local project is connected to GitHub
documentation files exist
Spring Boot backend is generated
initial setup is committed and pushed
major product decisions are documented
5. Phase 1 — Backend Foundation
Purpose

Build the backend foundation that all future features depend on.

This phase should create the structure and core backend architecture before adding complex business logic.

Tasks
Project Structure

Create backend packages:

config
controllers
dto
entities
enums
exceptions
repositories
security
services
Basic Configuration
Confirm Java 21.
Confirm Spring Boot 3.5.x.
Confirm Maven builds successfully.
Configure application properties for local development.
Add environment variable planning.
Add example configuration file later.
Health Check

Create:

GET /api/health

Purpose:

verify backend runs
help with deployment later
provide first simple endpoint
Initial Error Handling

Create:

GlobalExceptionHandler
ResourceNotFoundException
UnauthorizedAccessException later
DuplicateResourceException later
Initial DTO Strategy

Create starter request/response DTO structure.

Success Criteria

Phase 1 is complete when:

backend starts successfully
package structure exists
health endpoint works
initial error handling exists
backend can be tested locally
6. Phase 2 — Database Model and Entities
Purpose

Create the core data model that supports the Library Lane vision.

This phase is one of the most important parts of the project.

Core Rule
One Book = Unlimited Reading Experiences

This must be preserved.

V1 Entities

Create entities for:

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
Enums

Create enums for:

UserRole
BookFormat
ReadingStatus
ReadingExperienceType
SeriesStatus
MusicPlatform
MusicType
MusicAssociationType
GoalType
GoalStatus
JournalEntryType
Relationships

Implement relationships carefully:

User -> Library
User -> Books
Library -> Books
Book -> ReadingExperiences
Book -> Quotes
Book -> JournalEntries
Book -> MusicEntries
Book -> Collections
Book -> Authors
Book -> Genres
Book -> Series
Collection -> Books
Goal -> User
Goal -> Library
Join Tables

Create support for:

book_authors
book_genres
collection_books
Success Criteria

Phase 2 is complete when:

all V1 entities exist
enums exist
relationships compile
application starts without entity mapping errors
database schema can be generated locally
7. Phase 3 — PostgreSQL Local Setup
Purpose

Connect Library Lane to a real PostgreSQL database.

Tasks
Create local PostgreSQL database.
Configure Spring Boot database connection.
Add local environment variables or local application profile.
Confirm JPA creates tables.
Confirm backend connects successfully.
Decide whether to use schema auto-generation during development.
Decide whether to add Flyway later.
Development Database Name

Suggested:

library_lane_dev
Future Databases
library_lane_test
library_lane_prod
Success Criteria

Phase 3 is complete when:

backend connects to local PostgreSQL
tables are created
app starts cleanly
no credentials are committed to GitHub
8. Phase 4 — Authentication and User Libraries
Purpose

Add secure user accounts.

Library Lane requires authentication because every user has a private personal library.

Tasks
User Registration

Create:

POST /api/auth/register

Behavior:

validate email
validate password
hash password
create user
create default library
return JWT
Login

Create:

POST /api/auth/login

Behavior:

authenticate user
return JWT
return user summary
Current User

Create:

GET /api/auth/me

Behavior:

return authenticated user details
return library ID
Security

Implement:

Spring Security
JWT service
JWT filter
BCrypt password encoder
protected routes
CORS configuration
Success Criteria

Phase 4 is complete when:

users can register
users can log in
passwords are hashed
JWT authentication works
protected endpoints require authentication
each user gets a default library
9. Phase 5 — Core Book API
Purpose

Implement the core book management API.

Tasks

Create endpoints:

GET /api/books
GET /api/books/{bookId}
POST /api/books
PUT /api/books/{bookId}
DELETE /api/books/{bookId}
Book Features

Support:

title
subtitle
description
cover image URL
publication year
publisher
ISBN
page count
audiobook length
favorite flag
authors
genres
series
collections
initial reading experience
Search Fix

Implement case-insensitive search.

The capstone issue must not exist in the portfolio version.

Search should support:

title
author
series
genre
collection later
Success Criteria

Phase 5 is complete when:

authenticated users can add books
users can only see their own books
users can edit books
users can delete books
books can include authors and genres
search is case-insensitive
book detail endpoint supports Open Book View data
10. Phase 6 — Reading Experiences
Purpose

Implement unlimited reading experiences per book.

This is one of Library Lane’s biggest differentiators.

Tasks

Create endpoints:

GET /api/books/{bookId}/reading-experiences
POST /api/books/{bookId}/reading-experiences
PUT /api/reading-experiences/{experienceId}
PATCH /api/reading-experiences/{experienceId}/progress
DELETE /api/reading-experiences/{experienceId}
Features

Support:

first read
reread
first listen
relisten
DNF attempt
physical format
ebook format
audiobook format
current page
total pages
listening time
listening speed
percent complete
rating
review text
DNF reason
current active experience
Success Criteria

Phase 6 is complete when:

books can have unlimited reading experiences
reading experiences can have different formats
rereads do not overwrite previous reads
progress updates work
ratings can differ by experience
current reading experience can be identified
11. Phase 7 — Collections and Shelves
Purpose

Allow users to organize books into custom collections that feel like shelves.

Tasks

Create endpoints:

GET /api/collections
POST /api/collections
GET /api/collections/{collectionId}
PUT /api/collections/{collectionId}
DELETE /api/collections/{collectionId}
POST /api/collections/{collectionId}/books/{bookId}
DELETE /api/collections/{collectionId}/books/{bookId}
Features

Support:

collection name
description
optional theme
multiple books per collection
books in multiple collections
collection book count
future manual ordering
Success Criteria

Phase 7 is complete when:

users can create collections
users can add books to collections
users can remove books from collections
deleting a collection does not delete books
collection detail returns books
12. Phase 8 — Quotes and Journal Entries
Purpose

Add reflective reading features.

Quote Features

Create endpoints:

GET /api/books/{bookId}/quotes
POST /api/books/{bookId}/quotes
PUT /api/quotes/{quoteId}
DELETE /api/quotes/{quoteId}

Support:

quote text
page number
chapter
audiobook timestamp
speaker
note
spoiler flag
favorite flag
optional reading experience link
Journal Features

Create endpoints:

GET /api/journal
GET /api/books/{bookId}/journal
POST /api/books/{bookId}/journal
PUT /api/journal/{entryId}
DELETE /api/journal/{entryId}

Support:

title
body
entry type
page/chapter/timestamp
spoiler flag
optional reading experience link
Success Criteria

Phase 8 is complete when:

users can save quotes
users can save journal entries
quotes and journal entries connect to books
entries can optionally connect to reading experiences
users can edit and delete their own content
13. Phase 9 — Music Entries
Purpose

Add music as part of the Library Lane emotional experience.

Music should feel connected to the book, not like a random link.

Tasks

Create endpoints:

GET /api/books/{bookId}/music
GET /api/series/{seriesId}/music
POST /api/music
PUT /api/music/{musicEntryId}
DELETE /api/music/{musicEntryId}
Features

Support:

song
playlist
album
Spotify embed URL
external URL
book association
series association
reading experience association
scene association
character association
mood association
notes
Success Criteria

Phase 9 is complete when:

users can add music entries
Spotify embed links can be stored
music can connect to books
music can connect to reading experiences
music can connect to series
users can edit and delete music entries
14. Phase 10 — Goals
Purpose

Allow users to set reading goals.

Tasks

Create endpoints:

GET /api/goals
POST /api/goals
PUT /api/goals/{goalId}
DELETE /api/goals/{goalId}
GET /api/goals/{goalId}/progress
Goal Types

Support:

read X books
read X pages
listen to X audiobook hours
finish X TBR books
finish X series
read X books in a genre
reread X books
no-buy until TBR is under X
custom goal
Success Criteria

Phase 10 is complete when:

users can create goals
users can view active goals
goal progress can be calculated
completed goals can be marked
goals remain encouraging, not punitive
15. Phase 11 — Insights and Analytics
Purpose

Build meaningful analytics that help users understand their reading habits.

V1 Insights

Implement:

GET /api/insights/overview
GET /api/insights/genres
GET /api/insights/authors
GET /api/insights/formats
GET /api/insights/tbr
GET /api/insights/goals
Insight Types

Support:

total books
completed books
currently reading
TBR count
DNF count
average rating
total pages read
audiobook hours listened
total reading experiences
most-read genre
highest-rated genre
most-read author
format breakdown
goal progress
Success Criteria

Phase 11 is complete when:

insights display accurate data
analytics are user-scoped
reports are useful for the frontend
core insights support V1 dashboard/insights page
16. Phase 12 — External Book Metadata Search
Purpose

Reduce manual work during Add Story.

Tasks

Create:

GET /api/book-search?query=
GET /api/book-search/{source}/{externalId}
Providers

Evaluate:

Open Library
Google Books
Features

Support:

title search
author search
cover image
publication year
ISBN
page count
description
possible genre/category
Success Criteria

Phase 12 is complete when:

user can search external book metadata
search results return normalized response shape
user can select result during Add Story
selected result can populate book fields
17. Phase 13 — Frontend Foundation
Purpose

Create the React frontend foundation.

Tasks

Generate frontend with:

Vite
React
TypeScript

Install:

react-router-dom
@tanstack/react-query
react-hook-form
zod
@hookform/resolvers
framer-motion
recharts

Create frontend structure:

src/
  api/
  components/
  features/
  hooks/
  layouts/
  pages/
  routes/
  styles/
  types/
  utils/
Success Criteria

Phase 13 is complete when:

frontend runs locally
routes exist
app layout exists
API client is planned
basic pages exist
frontend connects to backend health endpoint
18. Phase 14 — Frontend Authentication
Purpose

Allow users to register, log in, and access protected pages.

Tasks

Create:

RegisterPage
LoginPage
AuthContext
protected routes
token storage
API auth header logic
logout behavior
Success Criteria

Phase 14 is complete when:

user can register from frontend
user can log in from frontend
token is stored
protected pages require auth
user can log out
19. Phase 15 — My Library and Bookshelf UI
Purpose

Create the main Library Lane experience.

The home page after login should be:

My Library

not a traditional dashboard.

Features

Build:

bookshelf scene
shelf rows
book cards/covers
current reads section
Add Story button
basic hover/focus interactions
responsive shelf layout
Success Criteria

Phase 15 is complete when:

authenticated user sees their library
books appear visually
covers display
empty state feels intentional
shelf is accessible
search/filter can locate books
20. Phase 16 — Add Story Flow
Purpose

Create the new Add Story experience.

Features

Support:

required title
required status
required format
book metadata search
manual add
cover selection basic
format-specific fields
collection selection
initial reading experience
save to shelf
Success Criteria

Phase 16 is complete when:

users can add books from frontend
users can search metadata
users can manually add
added book appears on shelf
fields adapt by format
21. Phase 17 — Book Added Animation
Purpose

Make adding a book feel special.

Features

Build:

new book visual response
book glide/fade into shelf
success message
reduced motion fallback
Success Criteria

Phase 17 is complete when:

newly added book visually joins shelf
animation is smooth
animation does not block functionality
reduced motion mode still works
22. Phase 18 — Open Book View
Purpose

Replace the capstone-style View page with a full Open Book View.

Features

Build:

book opening transition
large open-book style layout
overview spread
page-turn style navigation
sections for:
overview
reading experiences
progress
quotes
journal
music
collections
insights
Success Criteria

Phase 18 is complete when:

clicking shelf book opens Open Book View
current reading experience determines opening context
users can navigate sections
layout feels immersive
mobile fallback is usable
23. Phase 19 — Reading Experience UI
Purpose

Allow users to manage unlimited reading experiences visually.

Features

Build:

experience timeline
add new experience
update progress
mark completed
DNF/set aside
rating per experience
notes per experience
format-specific fields
Success Criteria

Phase 19 is complete when:

users can create multiple experiences
experiences are preserved separately
rereads do not overwrite old data
active/current read is clear
24. Phase 20 — Quotes, Journal, and Music UI
Purpose

Add the emotional and artistic layers of Library Lane.

Features

Build:

quote section
journal section
music section
Spotify embed display
add/edit/delete interactions
spoiler flag display
association labels
Success Criteria

Phase 20 is complete when:

users can save quotes
users can write journal entries
users can add music links/embeds
these sections feel integrated into Open Book View
25. Phase 21 — Collections UI
Purpose

Make collections feel like custom shelves.

Features

Build:

collections page
create collection
collection detail
add/remove books
collection shelf view
basic collection theme display
Success Criteria

Phase 21 is complete when:

users can create collections
users can organize books
collections display visually as shelves
books can belong to multiple collections
26. Phase 22 — Goals and Insights UI
Purpose

Create meaningful analytics and goal tracking.

Features

Build:

insights page
overview cards
genre chart
format chart
author stats
goal progress cards
TBR summary
warm/friendly copy
Success Criteria

Phase 22 is complete when:

users can view reading insights
charts display correctly
goals display progress
analytics feel personal, not corporate
27. Phase 23 — Testing
Purpose

Make Library Lane reliable and portfolio-ready.

Backend Tests

Add tests for:

auth
book service
reading experience service
collection service
goal progress
insight calculations
Frontend / E2E Tests

Add Playwright tests for:

register
login
add book
search book
update progress
create collection
add quote
add music
case-insensitive search
Success Criteria

Phase 23 is complete when:

major flows are tested
tests can run locally
README documents testing
28. Phase 24 — Deployment
Purpose

Deploy Library Lane as a live portfolio product.

Tasks
choose frontend host
choose backend host
choose PostgreSQL provider
configure environment variables
configure CORS
create production database
deploy backend
deploy frontend
verify live app
create demo account
Possible Providers

Frontend:

Vercel
Netlify

Backend:

Render
Railway
Fly.io

Database:

Neon
Supabase
Render PostgreSQL
Railway PostgreSQL
Success Criteria

Phase 24 is complete when:

live frontend works
live backend works
production database works
demo account works
no secrets are exposed
README has live links
29. Phase 25 — Portfolio Polish
Purpose

Prepare Library Lane for recruiters and portfolio visitors.

Tasks
update README
add screenshots
add GIFs
add architecture diagram
add API documentation
add database overview
add roadmap summary
add testing instructions
add deployment instructions
update personal portfolio website
add resume bullet points
Success Criteria

Phase 25 is complete when:

project looks polished on GitHub
README explains product and tech
screenshots show the visual experience
portfolio page links to app and repo
project can be confidently discussed in interviews
30. V1 Completion Definition

Library Lane V1 is complete when the user can:

Create an account.
Log in.
Add books manually.
Add books using metadata search.
View books on a visual shelf.
Open a book into Open Book View.
Track unlimited reading experiences.
Update progress by format.
Save quotes.
Write journal entries.
Add music entries.
Create collections.
Set reading goals.
View reading insights.
Use the app responsively.
Access a deployed live version.
Use a demo account.
View a polished GitHub README.
31. V1.5 Roadmap

After V1 works, add polish and deeper customization.

Features
shelf decorations
trinkets
better themes
richer page-turn animations
cover/edition selection
better music UI
reading progress logs
deeper analytics
tag/trope support
improved accessibility
improved mobile web layout
32. V2 Roadmap — Mobile App
Purpose

Create the first mobile app using the same backend.

Stack
React Native
Expo
TypeScript
Features
login
view current reads
search library
add book
update progress
add quote
add note
view collections
basic insights
Later Mobile Features
barcode scanning
cover scanning
quote photo capture
Google Play testing release
widgets
33. V3 Roadmap — Personalized Recommendations
Purpose

Add recommendation features based on the user’s actual reading patterns.

First Recommendation Types
choose from TBR
continue series
based on highest-rated genres
based on favorite authors
no-buy friendly recommendations from owned books
Future Recommendation Types
mood-based suggestions
trope-based recommendations
reread-aware recommendations
AI-assisted recommendation explanations
34. Future Dream Roadmap

Possible future features:

family libraries
child shelves
private fan art boards
mood boards
reading recaps
yearly wrapped
optional sharing
public shelf links
library room customization
seasonal environments
achievement decorations
bookish avatar/pet features

These should not distract from V1.

35. Current Next Steps

The immediate next steps are:

Finish planning documentation.
Confirm backend project structure.
Create backend packages.
Create enums.
Create first entities.
Configure PostgreSQL.
Build health endpoint.
Commit backend foundation.
Begin authentication.
36. Roadmap North Star

The roadmap should always protect the core identity of Library Lane:

Build the foundation first.
Make the library feel personal.
Preserve every reading experience.
Keep the product cozy, useful, and expandable.
Do not rush dream features before the core product works.