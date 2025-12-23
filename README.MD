# FitLife  
Enterprise Android Fitness & Collaboration Application

---

## Overview
**FitLife** is a production-ready Android application that enables users to manage fitness routines, exercises, and global task checklists, while supporting user delegation and real-time communication.

The application is built using an **offline-first architecture** with SQLite for core data persistence and Firebase for authentication and cloud-based collaboration. This design ensures reliability, performance, and scalability in real-world usage.

---

## Core Features

### Authentication & Session Management
- Secure user authentication using Firebase Authentication
- Local user mapping for offline functionality
- Persistent login sessions using SharedPreferences

### Routine Management
- Create, update, and delete fitness routines
- Gesture-based interactions (swipe actions)
- Soft delete with trash recovery
- Shake-to-restore the most recently deleted routine

### Exercise Management
- Add and edit exercises under routines
- Search and filter exercises
- Soft delete support
- Feature-scoped gesture handling

### Global Checklist
- Central checklist independent of routines
- Fast add, edit, and delete operations
- Soft delete with recovery support
- Shake-to-restore functionality

### Delegation & Collaboration
- User discovery by email
- Friend request and acceptance system
- Friends list management
- Task-oriented collaboration

### Real-Time Chat
- One-to-one chat using Firebase Firestore
- Checklist-context messaging
- Scalable Firestore data models

---

## Technology Stack

### Client
- Android SDK
- Java
- XML (Material Design)
- RecyclerView

### Backend & Services
- Firebase Authentication
- Firebase Firestore

### Local Persistence
- SQLite (DatabaseHelper)
- SharedPreferences (SessionManager)

### Build
- Gradle (Version Catalog)
- Minimum SDK: 24
- Target SDK: 36



## Helper & Utility Design

### Database Helper
**`DatabaseHelper`**
- Manages SQLite schema and versioning
- Handles CRUD operations
- Implements soft delete logic using timestamps
- Supports restore operations
- Maintains local user mapping

### Utility Helpers
**`SessionManager`**
- Stores login state and user identity
- Manages session expiration

**`ShareUtils`**
- Centralized message formatting
- SMS and intent-based sharing

### Feature-Scoped Helpers
Helpers that are feature-specific (e.g. gesture detectors) are intentionally placed within their respective feature packages to maintain separation of concerns.

---

## Data Strategy

- Firebase handles authentication and cloud-based communication
- SQLite is the primary store for routines, exercises, and checklist items
- Email acts as the bridge between Firebase users and local SQLite users
- All local data is user-scoped and isolated

---

## Setup & Run

1. Open the project in Android Studio
2. Sync Gradle dependencies
3. Add `google-services.json` for Firebase
4. Build and run on emulator or physical device

---

## Security Considerations
- Firebase handles credential security
- No plaintext password storage
- Session expiration enforced
- User data isolated per account

---

## Deployment Notes
- Designed for phones and tablets
- Offline functionality works without network
- Firebase rules must be configured for production

---

## Roadmap
- Cloud sync for local data
- Push notifications
- Analytics dashboard
- Role-based delegation
- Multi-device support

---

## Ownership
This project is intended for **internal use**.  
Architecture and structure are designed for maintainability and scalability.
