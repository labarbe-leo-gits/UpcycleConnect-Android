# UpcycleConnect

UpcycleConnect is a comprehensive Android application designed to facilitate upcycling by connecting users with containers, marketplace offers, and project management tools. It integrates security features like multi-factor authentication and leverages AI for content generation and impact estimation. It extends the WebApp available at the following link :
https://upcycleconnect.cloud/

## Technologies

* Language: Kotlin
* UI Framework: XML Layouts with Material Components and ConstraintLayout
* Networking: Retrofit 2 for REST API communication
* Serialization: Kotlinx Serialization
* Image Loading: Coil
* Payments: Stripe Android SDK
* Notifications: OneSignal
* Markdown: Markwon for rendering rich text
* AI: Integration with Google Gemini (Gemma model)
* Barcode Generation: Integration with bwipjs API

## Core Features

### Authentication and Security
* Secure login with multi-factor authentication (MFA).
* Support for MFA lifecycle: setup via QR codes, enabling with TOTP verification, and disabling.
* Session management and account protection.

### Container and Deposit Management
* Browsing available upcycling containers with location and capacity details.
* Detailed view of items within containers.
* Deposit management: Users can track their deposits, upload associated files, and monitor status.
* Barcode support: Each item in a container includes a unique barcode. Users can view, copy, or download barcodes as PNG or PDF.

### Marketplace and Offers
* Listing and browsing upcycling-related ads and offers (Annonces).
* Detailed ad views including pricing, material types, and seller ratings.
* Status management for ads, including approval and promotion tracking.

### Project Management (Updocs)
* Full CRUD operations for upcycling projects and their associated steps.
* Step-by-step project tracking with duration and material requirements.
* AI Integration: Assistant to help generate project content and detect AI-generated material.
* Social features: Support for project likes and community comments.
* Environmental Impact: Estimation of CO2 and energy impact based on material factors.

### Communication and Payments
* Real-time notifications via OneSignal integration.
* In-app notification management with read/unread status and deletion.
* Integrated checkout process using Stripe for handling orders and verifying payments.

### User Profile and Moderation
* User profile management including username and contact details updates.
* Account moderation features with ban status tracking.
* Secure account deletion process with MFA verification.

## API Architecture

The application communicates with several backend services:

### Main Api Service
* Users: Profile management, updates, and moderation.
* Containers: Retrieval of container lists, item details, and deposit status updates.
* Projects: Management of projects, steps, comments, and likes.
* Notifications: Retrieval and management of user-specific alerts.
* Payments: Creating and verifying Stripe payment intents and managing orders.

### Auth Api Service
* Dedicated service for the MFA lifecycle: setup, enabling, disabling, and verification.

### Gemini Api Service
* Integration with Google's generative AI models to provide smart content generation and AI detection.

## Project Structure

* Activities: Handle main UI flows like login, project editing, and checkout.
* Adapters: Manage data binding for lists of containers, notifications, and offers.
* Models: Data classes for API responses and internal state, using Kotlinx Serialization.
* Utils: Helper classes for session management, OneSignal routing, and barcode handling.
