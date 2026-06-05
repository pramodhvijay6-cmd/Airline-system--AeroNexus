# AeroNexus: Airline Reservation & Flight Management System

AeroNexus is a production-grade, portfolio-level full-stack application designed to simulate real-world airline scheduling, booking, real-time caching, dynamic pricing, and passenger flow management.

---

## Technical Architecture

AeroNexus is built on a highly decoupled multi-module architecture:
1. **Parent Module (`airline-parent`)**: Manages general build profiles and dependencies.
2. **Common Module (`airline-common`)**: Standardizes global exceptions, DTO structures, and shared enums.
3. **Domain Module (`airline-domain`)**: Manages the MySQL database connection, Flyway migrations, JPA entities, and repository layers.
4. **Service Module (`airline-service`)**: Contains business layers including real-time seat locking with Redis, loyalty transaction calculations, email notifications, and delay predictions.
5. **Web Module (`airline-web`)**: Implements REST controllers, Spring Security 6.x configs, rate-limiting handlers, and CORS rules.

### Tech Stack
* **Backend**: Java 21, Spring Boot 3.3.0, Spring Security, Hibernate/JPA, JWT, Redis Cache, Maven
* **Frontend**: React 18, Vite, Tailwind CSS, Redux Toolkit, Axios, Lucide Icons
* **Database & Cache**: MySQL 8.0, Redis 7.0 (alpine)
* **Reverse Proxy**: Nginx (handling client rate limiting and unified routing)
* **CI/CD**: GitHub Actions

---

## Key Features

1. **JWT & Security**: Stateless auth using Bearer access tokens and refresh tokens. Session caches and logout denylist managed in Redis.
2. **Real-time Seat Locking**: High-traffic seat selection is locked in Redis for 10 minutes using `SETNX` to prevent double-booking.
3. **Waitlist Queuing**: If seat capacity on a flight is full, booking requests are automatically pushed to a Redis-managed waitlist queue. Popped and cleared automatically if a confirmed traveler cancels.
4. **AI-Driven Delay Forecasting**: Rules-based prediction engine evaluating routes distance, scheduling densities, and departure peak-hours.
5. **Dynamic Pricing Engine**: Algorithmic multiplier factoring days remaining to departure, current occupancy rates, and holiday seasons.
6. **Baggage Management**: Integrated baggage registration (weigh-ins) and real-time transit status barcodes tracking.
7. **Personalized Recommendations**: Auto-suggests top 5 upcoming flights by scanning user travel history and identifying preferred destinations.
8. **Loyalty Ledger**: Earn points (1 point per $10 spent) and progress across Member Tiers (Bronze, Silver, Gold, Platinum).

---

## Environment Variables

| Variable | Default Value | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/airline_db` | MySQL connection URL |
| `DB_USERNAME` | `airline_user` | Database user account |
| `DB_PASSWORD` | `airline_password` | Database password |
| `REDIS_HOST` | `localhost` | Redis server hostname |
| `REDIS_PORT` | `6379` | Redis server port |
| `JWT_SECRET` | `5J2k9sD8f3hK...` | Secret key for JWT signatures |
| `JWT_EXPIRATION` | `86400000` | Access token duration (ms) |
| `JWT_REFRESH_EXPIRATION` | `604800000` | Refresh token duration (ms) |

---

## Quickstart

### Running via Docker Compose (Recommended)
Spin up the entire stack including MySQL, Redis, Nginx, the Java backend, and the React frontend:

```bash
docker-compose up --build -d
```

Access details:
* **Frontend Application**: `http://localhost/`
* **Swagger API Documentation**: `http://localhost/swagger-ui.html`
* **Spring Boot API Base**: `http://localhost/api/v1`

---

## API References Map

### Authentication (`/api/v1/auth`)
* `POST /register` - Creates a new user profile.
* `POST /login` - Processes user sign-in and returns access/refresh tokens.
* `POST /refresh` - Processes token rotation using a valid refresh token.
* `POST /logout` - Revokes access token by adding it to Redis denylist.
* `POST /forgot-password` - Requests reset link.
* `POST /reset-password` - Resets password using reset token.

### Flights Management (`/api/v1/flights`)
* `GET /` - List all flights.
* `GET /{id}` - Get flight details.
* `POST /` - Schedules a new flight (Admin/Staff only).
* `DELETE /{id}` - Cancels and deletes a flight record (Admin only).
* `GET /search` - Public flight search engine.

### Bookings (`/api/v1/bookings`)
* `POST /` - Starts booking (locks seats, creates pending booking).
* `POST /{id}/cancel` - Cancels booking, releases seats, processes waitlist.
* `GET /my` - Gets personal flight history logs.
* `GET /flight/{flightId}/occupied-seats` - Fetches live occupied seats map.

### Payments & Baggage (`/api/v1/payments`, `/api/v1/baggage`)
* `POST /payments/process` - Processes simulated card/UPI transactions and confirms bookings.
* `POST /baggage/check-in` - Registers passenger luggage tag.
* `GET /baggage/{bagTag}` - Tracks luggage status.

### AI & Administration (`/api/v1/ai`, `/api/v1/admin`)
* `GET /ai/predict-delay/{flightId}` - Returns scheduled flight delay forecasts.
* `GET /ai/recommendations` - Returns top-5 personalized flight recommendations.
* `GET /analytics` - Fetches revenue and occupancy statistics (Admin only).
* `GET /admin/audit-logs` - Views system activity changes (Admin only).
