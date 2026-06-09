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