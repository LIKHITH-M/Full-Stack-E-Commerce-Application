# Complete Steps to Start the Application

This guide contains exact commands to start the project **WITH Kafka** (full features like auto stock updates & order email notifications) and **WITHOUT Kafka** (lightweight mode).

---

## 🟢 OPTION 1: Running WITH Kafka (Full Event-Driven Mode)

Use this mode if you want **product stock to auto-update** and **order confirmation emails to be sent** after checkout.

### Step 1: Start Apache Kafka Server
Open **Terminal 1** in your Kafka installation directory (e.g., `C:\kafka`):

```powershell
# Format storage (only required once during initial Kafka setup)
.\bin\windows\kafka-storage.bat random-uuid
.\bin\windows\kafka-storage.bat format -t <GENERATED-UUID> -c .\config\server.properties

# Start Kafka Server (Keep this terminal running!)
.\bin\windows\kafka-server-start.bat .\config\server.properties
```

### Step 2: Enable Kafka Listeners in `application.properties`
Open `backend/src/main/resources/application.properties` and ensure line 5 is set to `true`:
```properties
spring.kafka.listener.auto-startup=true
```

### Step 3: Start Spring Boot Backend
Open **Terminal 2**:
```powershell
cd "backend"
.\mvnw.cmd clean install -DskipTests
.\mvnw.cmd spring-boot:run
```
*(Backend runs at: `http://localhost:8081`)*

### Step 4: Start React Frontend
Open **Terminal 3**:
```powershell
cd "frontend"
npm install    # (Only required once)
npm run dev
```
*(Frontend runs at: `http://localhost:5173`)*

---

## 🟡 OPTION 2: Running WITHOUT Kafka (Testing Mode)

Use this mode if you don't have Kafka running and want to quickly browse, add items to cart, and test checkout.

### Step 1: Disable Kafka Listeners in `application.properties`
Open `backend/src/main/resources/application.properties` and set line 5 to `false`:
```properties
spring.kafka.listener.auto-startup=false
```

### Step 2: Start Spring Boot Backend
Open **Terminal 1**:
```powershell
cd "backend"
.\mvnw.cmd spring-boot:run
```

### Step 3: Start React Frontend
Open **Terminal 2**:
```powershell
cd "frontend"
npm run dev
```

> **Note:** In Option 2, placing orders will succeed, but product stock counts won't auto-deduct and emails will not be sent.

---

## 🔑 Database & Admin User Setup

### 1. MySQL Database
Database `ecom_proj` is auto-created on application launch.
Check/update credentials in `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecom_proj?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root@123
```

### 2. Create Admin User
Register a user on the website, then run this SQL in MySQL to make them an admin:
```sql
USE ecom_proj;
UPDATE users SET role = 'ADMIN' WHERE username = 'your_username';
```
Log in again to access the Admin Panel at **http://localhost:5173/admin**.
