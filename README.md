# PaaSmart Backend

Hyperlocal delivery platform backend — a multi-vendor marketplace connecting local sellers, customers, and delivery partners in a single ecosystem, built with Spring Boot and PostgreSQL.

## Overview

PaaSmart lets local shop owners (clothing, food, general stores) sell online and get discovered by nearby customers. Customers browse, order, and track deliveries in real time; delivery partners pick up and deliver orders with OTP-verified handoffs; admins moderate sellers and monitor platform health.

## Tech Stack

- **Backend:** Java 24, Spring Boot 4.1, Spring Security, Spring Data JPA
- **Database:** PostgreSQL, Flyway (migrations)
- **Auth:** JWT (stateless, role-based access — Customer / Seller / Delivery / Admin)
- **Media Storage:** Cloudinary
- **Search:** PostgreSQL `pg_trgm` (fuzzy full-text search)
- **Notifications:** Expo Push Notifications
- **AI:** Swappable Virtual Try-On provider (Mock provider for development, Fashn.ai-ready for production)

## Features

- **Auth** — phone + OTP login, JWT-based sessions, role-based access control
- **Products & Shops** — seller catalog management, category browsing, nearby-shop discovery with distance sorting
- **Cart, Address & Checkout** — full shopping flow with COD/online payment modes
- **Orders** — sequential status flow (`PLACED → CONFIRMED → PREPARING → READY_FOR_PICKUP → PICKED_UP → IN_TRANSIT → DELIVERED → COMPLETED`)
- **Delivery Flow** — delivery partners accept, pick up, and confirm deliveries via OTP
- **Search** — fast fuzzy search across products and shops
- **Push Notifications** — real-time order status updates for customers, new-order alerts for sellers
- **Admin Panel** — seller approval/rejection, user moderation, platform-wide dashboard stats
- **Coupons** — percentage/flat discounts with usage limits and validity windows
- **Wallet & Referrals** — referral codes, sign-up/referral bonuses, wallet-based checkout
- **Product Reviews** — ratings, photo reviews, seller replies, helpful votes
- **AI Virtual Try-On** — customers try on clothing via photo upload (provider-agnostic architecture)
- **Seller Analytics** — order/revenue trends, top-selling products, ratings overview

## Project Structure

src/main/java/com/paasmart/backend/
├── auth/ # Registration, OTP login, JWT
├── seller/ # Shop management & seller dashboard
├── product/ # Product catalog
├── cart/ # Shopping cart
├── address/ # Customer addresses
├── checkout/ # Order placement
├── order/ # Order lifecycle & delivery-status transitions
├── delivery/ # Delivery partner endpoints
├── rating/ # Order-level ratings
├── review/ # Product-level reviews
├── wishlist/ # Wishlist + back-in-stock notifications
├── coupon/ # Discount coupons
├── wallet/ # Wallet balance & referral bonuses
├── search/ # Product/shop search
├── tryon/ # AI virtual try-on (swappable provider)
├── notification/ # Push notification service
├── admin/ # Admin moderation & dashboard
├── common/ # Cloudinary upload service
├── config/ # Security, JWT filter config
└── exception/ # Centralized error handling

## Getting Started

### Prerequisites
- Java 24
- PostgreSQL 18+
- Maven
- A [Cloudinary](https://cloudinary.com) account (free tier works)

### Setup

1. Clone the repository
2. Create a PostgreSQL database named `paasmart`
3. Copy `src/main/resources/application.properties.example` to `application.properties` and fill in your local values (database credentials, Cloudinary keys, JWT secret)
4. Run the app — Flyway will automatically apply all database migrations on startup:

./mvnw spring-boot:run

5. The API will be available at `http://localhost:8080`

### Environment Variables

The following must be configured in `application.properties` (see `.example` file):

| Property | Description |
|---|---|
| `spring.datasource.url` / `username` / `password` | PostgreSQL connection |
| `cloudinary.cloud-name` / `api-key` / `api-secret` | Media uploads |
| `jwt.secret` | JWT signing key |
| `fashn.ai.api-key` | Optional — only needed when switching from the mock try-on provider to Fashn.ai |

## API Documentation

All endpoints are prefixed by role/module:
- `/api/v1/auth/**` — registration & login
- `/api/v1/seller/**` — seller-only operations
- `/api/v1/admin/**` — admin-only operations
- `/api/delivery/**` — delivery-partner operations
- `/api/cart`, `/api/address`, `/api/checkout`, `/api/wallet`, `/api/coupons`, `/api/tryon` — authenticated customer operations
- `/api/v1/products`, `/api/v1/shops`, `/api/search` — public browsing endpoints

## Status

Actively under development. Core marketplace flow (auth → browse → cart → checkout → delivery → review) is complete and tested end-to-end. Customer, seller, and delivery mobile apps are being built against this API.

## License

Private project — all rights reserved.