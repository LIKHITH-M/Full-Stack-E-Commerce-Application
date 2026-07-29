# Complete Steps to Run & Verify the E-Commerce Application

This guide covers running the project in **two modes** and verifying every component works correctly.

---

## Prerequisites

Ensure you have the following installed before proceeding:

| Tool | Check Command | Required For |
|------|---------------|-------------|
| Java 21 JDK | `java -version` | Backend |
| Maven | `mvn -version` | Backend |
| Node.js v18/v20+ | `node -v` | Frontend |
| npm | `npm -v` | Frontend |
| MySQL Server | Running on port `3306` | Database |
| Docker Desktop | `docker --version` | Prometheus & Grafana (Optional) |

---

## Database Setup (One-Time)

1. MySQL database `ecom_proj` is **auto-created** on first app launch.
2. Verify credentials in `backend/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/ecom_proj?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=root@123
   ```
3. After registering a user on the website, promote them to Admin:
   ```sql
   USE ecom_proj;
   UPDATE users SET role = 'ADMIN' WHERE username = 'your_username';
   ```
   Log in again to access the Admin Panel at `http://localhost:5173/admin`.

---

## Phase 1: Run WITHOUT Kafka (Lightweight Mode)

> Use this mode to test core features: Product CRUD, User Auth, Cart, Checkout, Actuator Metrics, Unit Tests, and E2E Tests — all without Kafka overhead.

### What Works in This Mode

| Feature | Status |
|---------|--------|
| Product Browsing, Search, Categories | ✅ Works |
| User Registration & JWT Login | ✅ Works |
| Add to Cart & Razorpay Checkout | ✅ Works |
| Admin Panel (Add/Edit/Delete Products) | ✅ Works |
| Spring Boot Actuator Health & Prometheus | ✅ Works |
| JUnit 5 + Mockito Backend Tests | ✅ Works |
| Playwright Frontend E2E Tests | ✅ Works |
| Prometheus & Grafana Dashboards | ✅ Works |
| Auto Stock Deduction after Order | ❌ Skipped (needs Kafka) |
| Order Confirmation Email | ❌ Skipped (needs Kafka) |

---

### Step 1: Run Backend Unit Tests

These tests run in **complete isolation** — no MySQL, no Kafka, no server needed.

**Terminal 1:**
```powershell
cd backend
mvn clean test
```

**✅ Verification:**
- Output shows `Tests run: X, Failures: 0, Errors: 0`
- Final line: `BUILD SUCCESS`
- Tests executed: `ProductServiceTest` (5 tests) + `OrderEventProducerTest` (2 tests)

---

### Step 2: Start Spring Boot Backend

**Terminal 1:**
```powershell
cd backend
mvn clean spring-boot:run
```

> The default `application.properties` has `spring.kafka.listener.auto-startup=${KAFKA_ENABLED:false}`, so Kafka consumers will NOT start. No Kafka warning logs will appear.

**✅ Verification — Open these URLs in your browser:**

| URL | Expected Result |
|-----|----------------|
| `http://localhost:8081/actuator/health` | `{"status":"UP","components":{"db":{"status":"UP",...},...}}` |
| `http://localhost:8081/actuator/prometheus` | Long plaintext output with metrics like `jvm_memory_used_bytes`, `hikaricp_connections_active`, `http_server_requests_seconds` |
| `http://localhost:8081/api/products` | JSON array of products (empty `[]` if no products added yet) |

---

### Step 3: Start React Frontend

**Terminal 2:**
```powershell
cd frontend
npm install       # Only needed first time
npm run dev
```

**✅ Verification:**
- Open `http://localhost:5173` in your browser
- Browse products, use the search bar
- Register a new user → Login → Add items to cart
- Proceed to checkout (Razorpay payment flow)
- Order is saved in MySQL ✅ but stock does NOT auto-deduct and email is NOT sent (Kafka is off)

---

### Step 4: Run Playwright E2E Tests

**Terminal 3** (keep backend + frontend running):
```powershell
cd frontend
npx playwright install --with-deps    # Only needed first time
npx playwright test
```

**✅ Verification:**
- Output: `3 passed`
- To view detailed HTML report: `npx playwright show-report`
- To run in interactive UI mode: `npx playwright test --ui`

---

### Step 5: Start Prometheus & Grafana (Optional — requires Docker)

**Terminal 4** (from project root):
```powershell
docker compose -f docker-compose.monitoring.yml up -d
```

**✅ Verification:**

| URL | Expected Result |
|-----|----------------|
| `http://localhost:9090/targets` | Prometheus target `spring-boot-backend` shows State: **UP** |
| `http://localhost:3000` | Grafana login page. Login: `admin` / `admin` |

**Grafana Setup:**
1. Go to **Connections → Data Sources → Add data source → Prometheus**
2. Set URL to `http://prometheus:9090`
3. Click **Save & test** → should show ✅ success
4. Import dashboard ID `11378` (Spring Boot community dashboard) for JVM, HTTP, and DB metrics

---

### Step 6: Verify GitHub Actions CI (After Pushing to GitHub)

```powershell
git add .
git commit -m "feat: add automated testing, Playwright E2E, and observability"
git push origin main
```

**✅ Verification:**
- Go to GitHub → **Actions** tab
- Workflow **Full-Stack E-Commerce CI Automated Test Suite** should run
- Both jobs show green checkmarks ✅:
  - `backend-unit-tests` (runs `mvn test`)
  - `frontend-e2e-tests` (runs `npx playwright test`)

---
---

## Phase 2: Run WITH Kafka (Full Event-Driven Mode)

> Use this mode to test the complete event-driven architecture: when a user places an order, Kafka triggers automatic stock updates and sends confirmation emails.

### What Additional Features Kafka Enables

| Feature | Without Kafka | With Kafka |
|---------|:---:|:---:|
| Auto Stock Deduction after Order | ❌ | ✅ `InventoryUpdateConsumer` decrements `stock_quantity` in MySQL |
| Order Confirmation Email | ❌ | ✅ `EmailNotificationConsumer` sends email via SMTP |
| Kafka Consumer Lag Metrics in Grafana | ❌ | ✅ Real-time consumer lag monitoring |

---

### Step 1: Start Kafka Broker

**Option A — Using Docker (Recommended, simplest):**
```powershell
docker run -d --name kafka-broker -p 9092:9092 apache/kafka:latest
```

**Option B — Using Local Kafka Installation:**

**Terminal 1** (navigate to your Kafka installation directory, e.g., `C:\kafka`):
```powershell
# Format storage (only required once during initial Kafka setup)
.\bin\windows\kafka-storage.bat random-uuid
.\bin\windows\kafka-storage.bat format -t <GENERATED-UUID> -c .\config\server.properties

# Start Kafka Server (keep this terminal running!)
.\bin\windows\kafka-server-start.bat .\config\server.properties
```

**✅ Verification:**
- Kafka should be listening on `localhost:9092`
- If using Docker: `docker logs kafka-broker` should show `Kafka Server started`

---

### Step 2: Start Spring Boot Backend with Kafka Enabled

**Terminal 2:**
```powershell
cd backend
$env:KAFKA_ENABLED="true"
mvn clean spring-boot:run
```

> Setting `$env:KAFKA_ENABLED="true"` overrides the default `false` in `application.properties` and starts Kafka consumers.

**✅ Verification — Check the Spring Boot startup logs for these lines:**
```
✅ KafkaMessageListenerContainer started (email-group)
✅ KafkaMessageListenerContainer started (inventory-group)
```

Also verify Actuator endpoints still work:

| URL | Expected Result |
|-----|----------------|
| `http://localhost:8081/actuator/health` | `{"status":"UP"}` — no Kafka warning logs in terminal |
| `http://localhost:8081/actuator/prometheus` | Metrics now include `kafka_consumer_*` entries |

---

### Step 3: Start React Frontend

**Terminal 3:**
```powershell
cd frontend
npm run dev
```

---

### Step 4: Test the Full Event-Driven Order Flow

1. Open `http://localhost:5173` in your browser
2. Login → Add products to cart → Proceed to Checkout → Complete Razorpay Payment
3. After order is placed successfully:

**✅ Verification — Check Spring Boot terminal logs:**
```
✅ Published OrderPlacedEvent to Kafka for order: order_xxxxx
✅ [EmailNotificationConsumer] Sending order confirmation email to: user@example.com
✅ [InventoryUpdateConsumer] Updated stock for product: Wireless Mouse (quantity: -2)
```

**✅ Verification — Check MySQL Database:**
```sql
USE ecom_proj;
SELECT name, stock_quantity FROM product WHERE name = 'Wireless Mouse';
```
Stock quantity should be **automatically decremented** by the ordered amount.

**✅ Verification — Check Email (if SMTP configured):**
- The registered user's email should receive an order confirmation email
- Requires valid SMTP credentials in `application.properties`:
  ```properties
  spring.mail.username=your_real_email@gmail.com
  spring.mail.password=your_google_app_password
  ```

---

### Step 5: Run All Tests Again (With Kafka Running)

**Backend Tests:**
```powershell
cd backend
mvn clean test
```
- All tests should still pass (`BUILD SUCCESS`)
- `OrderEventProducerTest` verifies Kafka publish logic and graceful degradation

**Frontend E2E Tests:**
```powershell
cd frontend
npx playwright test
```
- All 3 specs should still pass

---

### Step 6: Monitor Kafka Metrics in Grafana

If Prometheus & Grafana are running (`docker compose -f docker-compose.monitoring.yml up -d`):

1. Open Grafana at `http://localhost:3000`
2. In **Explore**, query these Prometheus metrics:
   - `kafka_consumer_fetch_manager_records_consumed_total` — total messages consumed
   - `kafka_consumer_fetch_manager_records_lag` — consumer lag (messages waiting)
   - `kafka_consumer_coordinator_rebalance_total` — consumer group rebalances

---

## Quick Reference: Switching Between Modes

| Action | Command |
|--------|---------|
| **Run WITHOUT Kafka** | `mvn clean spring-boot:run` (default `KAFKA_ENABLED=false`) |
| **Run WITH Kafka** | `$env:KAFKA_ENABLED="true"; mvn clean spring-boot:run` |
| **Start Kafka via Docker** | `docker run -d --name kafka-broker -p 9092:9092 apache/kafka:latest` |
| **Stop Kafka Docker** | `docker stop kafka-broker` |
| **Run Backend Tests** | `cd backend && mvn clean test` |
| **Run Frontend E2E Tests** | `cd frontend && npx playwright test` |
| **Start Monitoring Stack** | `docker compose -f docker-compose.monitoring.yml up -d` |
| **Stop Monitoring Stack** | `docker compose -f docker-compose.monitoring.yml down` |