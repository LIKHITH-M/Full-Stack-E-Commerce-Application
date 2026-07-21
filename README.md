# 🛒 Full Stack E-Commerce Application

![Stack](https://img.shields.io/badge/Stack-Spring_Boot_+_React-blueviolet?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-5-646CFF?style=flat-square&logo=vite&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8+-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka-KRaft-231F20?style=flat-square&logo=apachekafka&logoColor=white)
![Razorpay](https://img.shields.io/badge/Payments-Razorpay-002970?style=flat-square&logo=razorpay&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

**A production-ready, full-stack e-commerce platform** built with **Spring Boot 3 (Java 21)** on the backend and **React 18 (Vite)** on the frontend. It features JWT-based stateless authentication, **Razorpay** payment gateway integration, and an **event-driven architecture** powered by **Apache Kafka (KRaft mode)** for real-time order notifications and automated inventory management.

The application provides a complete shopping experience for users — browsing, searching, cart management, and secure checkout — alongside a powerful admin dashboard for managing products, categories, users, and orders.

---

## 🏗️ Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT (Browser)                                    │
│                         React 18 + Vite + Bootstrap 5                            │
│                                                                                  │
│   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│   │  Login /  │  │   Home   │  │   Cart   │  │ Product  │  │  Admin Dashboard │  │
│   │ Register │  │  Browse  │  │ Checkout │  │  Detail  │  │  (CRUD Panels)   │  │
│   └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────────┬─────────┘  │
│        └──────────────┴──────────────┴──────────────┴───────────────┘             │
│                                     │  Axios + JWT Interceptor                   │
└─────────────────────────────────────┼────────────────────────────────────────────┘
                                      │ REST API (HTTP/JSON)
┌─────────────────────────────────────┼────────────────────────────────────────────┐
│                          SPRING BOOT 3 (Java 21)                                 │
│                                     │                                            │
│   ┌──────────────────────────────── │ ───────────────────────────────────────┐   │
│   │                      Spring Security (JWT Filter)                        │   │
│   │                   BCrypt Password Hashing + Role-Based Access            │   │
│   └──────────────────────────────── │ ───────────────────────────────────────┘   │
│                                     │                                            │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐    │
│   │    Auth      │  │   Product    │  │   Payment    │  │   Order          │    │
│   │  Controller  │  │  Controller  │  │  Controller  │  │  Controller      │    │
│   └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────────┘    │
│          │                 │                 │                 │                  │
│   ┌──────┴───────┐  ┌──────┴───────┐  ┌──────┴───────┐  ┌──────┴───────────┐    │
│   │  UserService │  │ProductService│  │RazorPayService│  │OrderEventProducer│    │
│   │  JwtService  │  │CategoryService│ │              │  │                  │    │
│   └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────────┘    │
│          │                 │                 │                 │                  │
│   ┌──────┴─────────────────┴─────────────────┴─────────┐       │                 │
│   │                Spring Data JPA                      │       │                 │
│   │          (UserRepo, ProductRepo, OrderRepo,         │       │                 │
│   │           CategoryRepo)                             │       │                 │
│   └──────────────────────┬──────────────────────────────┘       │                 │
│                          │                                      │                 │
└──────────────────────────┼──────────────────────────────────────┼─────────────────┘
                           │                                      │
              ┌────────────▼────────────┐            ┌────────────▼────────────────┐
              │     MySQL / PostgreSQL  │            │     Apache Kafka (KRaft)    │
              │                         │            │                             │
              │  ┌───────┐ ┌──────────┐ │            │  Topic: order-placed-topic  │
              │  │ users │ │ products │ │            │                             │
              │  └───────┘ └──────────┘ │            │  ┌───────────────────────┐  │
              │  ┌────────┐ ┌────────┐  │            │  │ EmailNotification     │  │
              │  │ orders │ │category│  │            │  │ Consumer              │──┼──▶ SMTP Email
              │  └────────┘ └────────┘  │            │  └───────────────────────┘  │
              │                         │            │  ┌───────────────────────┐  │
              └─────────────────────────┘            │  │ InventoryUpdate       │  │
                                                     │  │ Consumer              │──┼──▶ Stock Update
                                                     │  └───────────────────────┘  │
                                                     └────────────────────────────┘
```

---

## ⚙️ Tech Stack

### Backend

| Technology | Purpose |
|:---|:---|
| **Java 21** | Core language with modern features (records, pattern matching, virtual threads) |
| **Spring Boot 3.3** | Application framework with auto-configuration and embedded Tomcat |
| **Spring Security** | Authentication & authorization with JWT-based stateless sessions |
| **Spring Data JPA** | ORM layer with Hibernate for database operations |
| **Spring Kafka** | Kafka producer/consumer integration for event-driven messaging |
| **Spring Mail** | SMTP-based email notifications (Gmail) |
| **JJWT (0.12.6)** | JWT token generation, signing, and validation |
| **Razorpay Java SDK** | Server-side payment order creation and verification |
| **Lombok** | Boilerplate reduction — `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` |
| **MySQL Connector/J** | JDBC driver for MySQL database connectivity |
| **PostgreSQL Driver** | JDBC driver for PostgreSQL (switchable alternative) |
| **Maven** | Build tool and dependency management |

### Frontend

| Technology | Purpose |
|:---|:---|
| **React 18** | Component-based UI library with hooks and context API |
| **Vite 5** | Lightning-fast dev server with HMR and optimized builds |
| **React Router DOM 6** | Client-side routing with protected route guards |
| **Axios** | HTTP client with JWT interceptor for automatic token injection |
| **Bootstrap 5** | Responsive UI framework with grid system and components |
| **React Bootstrap** | Bootstrap components as React elements |
| **Bootstrap Icons** | Icon library for UI elements |
| **React Icons** | Additional icon sets for enhanced visual design |
| **Sass** | CSS preprocessor for custom styling |
| **SWC** | Rust-based compiler for fast JSX/TSX transpilation |

### Infrastructure & Services

| Technology | Purpose |
|:---|:---|
| **Apache Kafka (KRaft)** | Event streaming platform — no ZooKeeper dependency |
| **MySQL 8+** | Primary relational database (auto-creates schema) |
| **PostgreSQL** | Alternative database (switchable via config) |
| **Razorpay** | Payment gateway for Indian Rupee transactions |
| **Gmail SMTP** | Transactional email delivery for order confirmations |

---

## 🌟 Key Features

### 🛍️ User Shopping Experience

- **JWT Authentication** — Stateless, secure register/login with BCrypt password hashing and role-based access tokens
- **Product Browsing** — Browse all products with image rendering, category-based filtering, and real-time keyword search
- **Shopping Cart** — Add products with quantity management, dynamic price calculation, and persistent cart state
- **Secure Checkout** — Integrated Razorpay payment gateway with order creation, payment verification, and confirmation
- **Order History** — View past orders with payment IDs, timestamps, and itemized details

### 🔐 Enterprise-Grade Security

- **JWT Filter Chain** — Every request passes through a custom `JwtFilter` that validates tokens before reaching controllers
- **Role-Based Access Control** — `USER` and `ADMIN` roles with Spring Security's `hasRole()` authorization
- **BCrypt Hashing** — Passwords stored with BCrypt (strength 12) — never in plain text
- **CORS Configuration** — Restricted cross-origin access to authorized frontend origins only
- **Auto-Redirect on 401/403** — Axios interceptor automatically clears tokens and redirects to login on unauthorized responses

### 👨‍💼 Admin Dashboard (`/admin`)

- **Category Management** — Full CRUD operations for product categories
- **Product Management** — Create, read, update, delete products with multipart image upload
- **User Management** — View all registered users and delete accounts
- **Order Management** — View all orders across all users with payment details
- **Stock Tracking** — Inventory automatically updated via Kafka consumers on order placement

### 📨 Event-Driven Architecture (Apache Kafka)

- **`OrderEventProducer`** — Publishes `OrderPlacedEvent` to `order-placed-topic` upon successful checkout
- **`EmailNotificationConsumer`** — Listens to the topic and sends order confirmation emails via SMTP
- **`InventoryUpdateConsumer`** — Listens to the same topic and automatically reduces product stock quantities
- **Fault-Tolerant** — Kafka is configured with `fatalIfBrokerNotAvailable=false` so the app starts even without Kafka running

---

## 📁 Project Structure

```
Full-Stack-E-Commerce-Application/
├── backend/                                    # Spring Boot REST API
│   ├── src/main/java/com/likhith/ecomproj/
│   │   ├── config/                             # Security, JWT & Kafka configuration
│   │   │   ├── SecurityConfig.java             # Spring Security filter chain & CORS
│   │   │   ├── JwtFilter.java                  # JWT token validation filter
│   │   │   └── KafkaConfig.java                # Kafka producer factory & admin
│   │   ├── controller/                         # REST API endpoint handlers
│   │   │   ├── AuthController.java             # /api/auth — Register & Login
│   │   │   ├── ProductController.java          # /api/product(s) — Product CRUD & Search
│   │   │   ├── CategoryController.java         # /api/categories — Public category listing
│   │   │   ├── PaymentController.java          # /api/payments — Razorpay order creation
│   │   │   ├── OrderController.java            # /api/orders — Checkout & Order history
│   │   │   ├── AdminCategoryController.java    # /api/admin/categories — Admin category CRUD
│   │   │   ├── AdminUserController.java        # /api/admin/users — Admin user management
│   │   │   └── TestEmailController.java        # /api/test — Email debugging endpoint
│   │   ├── model/                              # JPA Entities & DTOs
│   │   │   ├── User.java                       # User entity (id, username, password, email, role)
│   │   │   ├── Product.java                    # Product entity (with image BLOB storage)
│   │   │   ├── Category.java                   # Category entity
│   │   │   ├── OrderEntity.java                # Persisted order record
│   │   │   └── OrderPlacedEvent.java           # Kafka event DTO with nested OrderItem
│   │   ├── repo/                               # Spring Data JPA Repositories
│   │   │   ├── UserRepo.java                   # User database queries
│   │   │   ├── ProductRepo.java                # Product queries (search by keyword)
│   │   │   ├── CategoryRepo.java               # Category database queries
│   │   │   └── OrderRepo.java                  # Order queries (by username)
│   │   └── service/                            # Business logic & Kafka producers/consumers
│   │       ├── UserService.java                # User registration & lookup
│   │       ├── ProductService.java             # Product CRUD with image handling
│   │       ├── CategoryService.java            # Category CRUD operations
│   │       ├── JwtService.java                 # JWT generation, validation & claims extraction
│   │       ├── MyUserDetailsService.java       # Spring Security UserDetailsService impl
│   │       ├── RazorPayService.java            # Razorpay order creation via SDK
│   │       ├── OrderEventProducer.java         # Kafka producer — publishes OrderPlacedEvent
│   │       ├── EmailNotificationConsumer.java  # Kafka consumer — sends confirmation emails
│   │       └── InventoryUpdateConsumer.java    # Kafka consumer — reduces product stock
│   ├── src/main/resources/
│   │   └── application.properties              # DB, Kafka, Razorpay, SMTP configuration
│   └── pom.xml                                 # Maven dependencies & build config
│
├── frontend/                                   # React + Vite SPA
│   ├── public/                                 # Static assets & HTML template
│   ├── src/
│   │   ├── components/                         # UI Components
│   │   │   ├── Home.jsx                        # Product grid with category filter & search
│   │   │   ├── Navbar.jsx                      # Navigation bar with category dropdown
│   │   │   ├── Product.jsx                     # Single product detail view
│   │   │   ├── Cart.jsx                        # Shopping cart with Razorpay checkout
│   │   │   ├── CheckoutPopup.jsx               # Checkout confirmation modal
│   │   │   ├── Login.jsx                       # Login & Registration forms
│   │   │   ├── AddProduct.jsx                  # Admin — Add new product form
│   │   │   ├── UpdateProduct.jsx               # Admin — Edit existing product
│   │   │   └── admin/                          # Admin Dashboard components
│   │   │       ├── AdminDashboard.jsx          # Admin layout with sidebar navigation
│   │   │       ├── AdminCategories.jsx         # Category management panel
│   │   │       ├── AdminProducts.jsx           # Product management panel
│   │   │       ├── AdminUsers.jsx              # User management panel
│   │   │       └── AdminOrders.jsx             # Order management panel
│   │   ├── Context/
│   │   │   └── Context.jsx                     # React Context for global state management
│   │   ├── axios.jsx                           # Axios instance with JWT interceptor
│   │   ├── App.jsx                             # Root component with routing & route guards
│   │   ├── App.css                             # Global application styles
│   │   ├── index.css                           # Root CSS reset
│   │   └── main.jsx                            # React DOM entry point
│   ├── index.html                              # HTML template
│   ├── vite.config.js                          # Vite configuration
│   └── package.json                            # NPM dependencies & scripts
│
├── .gitignore                                  # Git ignore rules (node_modules, target, IDE files)
├── EMAIL_SETUP.md                              # Email configuration guide
├── RUN_STEPS.md                                # Step-by-step run instructions
└── README.md                                   # Project documentation (this file)
```

---

## 🔌 API Endpoints Reference

### Authentication (`/api/auth`)

| Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---:|
| `POST` | `/register` | Create a new user account | No |
| `POST` | `/login` | Authenticate and receive JWT token | No |

### Products (`/api`)

| Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---:|
| `GET` | `/products` | Retrieve all products | No |
| `GET` | `/product/:id` | Get single product by ID | No |
| `GET` | `/product/:id/image` | Get product image binary | No |
| `GET` | `/products/search?keyword=` | Search products by keyword | No |
| `POST` | `/product` | Add a new product (multipart) | Admin |
| `PUT` | `/product/:id` | Update product details and image | Admin |
| `DELETE` | `/product/:id` | Delete a product | Admin |

### Categories (`/api/categories`)

| Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---:|
| `GET` | `/` | List all categories | Yes |

### Payments (`/api/payments`)

| Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---:|
| `POST` | `/create-order` | Create Razorpay payment order | Yes |

### Orders (`/api/orders`)

| Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---:|
| `POST` | `/checkout` | Place order after payment success | Yes |
| `GET` | `/my-orders` | Get logged-in user's orders | Yes |
| `GET` | `/all` | Get all orders (admin view) | Admin |

### Admin — Categories (`/api/admin/categories`)

| Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---:|
| `GET` | `/` | List all categories | Admin |
| `GET` | `/:id` | Get category by ID | Admin |
| `POST` | `/` | Create a new category | Admin |
| `PUT` | `/:id` | Update a category | Admin |
| `DELETE` | `/:id` | Delete a category | Admin |

### Admin — Users (`/api/admin/users`)

| Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---:|
| `GET` | `/` | List all users | Admin |
| `GET` | `/:id` | Get user by ID | Admin |
| `DELETE` | `/:id` | Delete a user | Admin |

---

## 🚀 Getting Started

### Prerequisites

| Requirement | Version |
|:---|:---|
| Java | 21+ |
| Node.js | 18+ |
| MySQL | 8+ |
| Apache Kafka | 3.x (KRaft mode) |
| Maven | 3.8+ (or use included `mvnw`) |

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/<your-username>/Full-Stack-E-Commerce-Application.git
cd Full-Stack-E-Commerce-Application
```

### 2️⃣ Database Setup

MySQL is the default database. The schema `ecom_proj` will be **auto-created** on first run.

```bash
# Ensure MySQL is running on port 3306
# Default credentials in application.properties:
#   username: root
#   password: root@123
```

> **Switching to PostgreSQL?** Uncomment the PostgreSQL section and comment out MySQL in `backend/src/main/resources/application.properties`.

### 3️⃣ Kafka Setup (KRaft Mode — No ZooKeeper)

```bash
# Generate a cluster UUID
kafka-storage.bat random-uuid

# Format the log directory
kafka-storage.bat format -t <GENERATED_UUID> -c config/kraft/server.properties

# Start the Kafka broker
kafka-server-start.bat config/kraft/server.properties
```

> **Note:** The app is configured to start even without Kafka running (`fatalIfBrokerNotAvailable=false`). Kafka features (email notifications, auto stock update) will be disabled until Kafka is available.

### 4️⃣ Backend Setup

```bash
cd backend

# Update application.properties with your credentials:
#   - Razorpay API key & secret
#   - Gmail address & app password
#   - Database credentials (if different from defaults)

# Build and run
mvn clean install -DskipTests
mvn spring-boot:run
```

> Backend starts at: **`http://localhost:8081`**

### 5️⃣ Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

> Frontend starts at: **`http://localhost:5173`**

### 6️⃣ Create an Admin User

Register a user through the app normally, then promote them to admin:

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'your_admin_username';
```

---

## 🔧 Configuration

All backend configuration is centralized in `backend/src/main/resources/application.properties`:

| Property | Description | Default |
|:---|:---|:---|
| `server.port` | Backend API port | `8081` |
| `spring.datasource.url` | Database JDBC URL | `jdbc:mysql://localhost:3306/ecom_proj` |
| `spring.datasource.username` | Database username | `root` |
| `spring.datasource.password` | Database password | `root@123` |
| `razorpay.api.key` | Razorpay public key | *(your key)* |
| `razorpay.api.secret` | Razorpay secret key | *(your secret)* |
| `spring.kafka.bootstrap-servers` | Kafka broker address | `localhost:9092` |
| `spring.mail.username` | Gmail address for sending emails | *(your email)* |
| `spring.mail.password` | Gmail App Password (not regular password) | *(your app password)* |

> ⚠️ **Security**: Never commit real API keys or passwords to Git. Use environment variables or a `.env` file for production deployments.

---

## 📐 Frontend Routes

| Path | Component | Access | Description |
|:---|:---|:---|:---|
| `/login` | `Login` | Public | Login and registration page |
| `/` | `Home` | User | Product grid with search and category filter |
| `/product/:id` | `Product` | User | Single product detail view |
| `/cart` | `Cart` | User | Shopping cart with checkout |
| `/admin` | `AdminDashboard` | Admin | Admin panel (redirects to `/admin/categories`) |
| `/admin/categories` | `AdminCategories` | Admin | Category management |
| `/admin/products` | `AdminProducts` | Admin | Product management |
| `/admin/users` | `AdminUsers` | Admin | User management |
| `/admin/orders` | `AdminOrders` | Admin | Order management |
| `/add_product` | `AddProduct` | Admin | Add new product form |
| `/product/update/:id` | `UpdateProduct` | Admin | Edit product form |

---

## 📨 Event-Driven Flow (Kafka)

```
User completes Razorpay payment
        │
        ▼
OrderController.checkout()
        │
        ├──▶ Save OrderEntity to MySQL
        │
        ├──▶ OrderEventProducer.publishOrderPlacedEvent()
        │         │
        │         ▼
        │    Kafka Topic: "order-placed-topic"
        │         │
        │         ├──▶ EmailNotificationConsumer
        │         │         └──▶ Sends confirmation email via Gmail SMTP
        │         │
        │         └──▶ InventoryUpdateConsumer
        │                   └──▶ Reduces product stock in database
        │
        └──▶ Direct email fallback (bypasses Kafka if needed)
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Built by <strong>Likhith</strong>
</p>
