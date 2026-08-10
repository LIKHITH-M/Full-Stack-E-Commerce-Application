# 🚀 Complete Production Deployment Guide

This guide details the step-by-step process of deploying the **Full-Stack E-Commerce Application** to cloud infrastructure using **Vercel** (Frontend), **Render** (Backend API), **Neon PostgreSQL** (Database), **Aiven Apache Kafka** (Event Messaging), and **Grafana Cloud** (Monitoring).

---

## 🌐 Production Architecture

```
                  ┌──────────────────────┐
                  │    User Browser      │
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │   Vercel (Frontend)  │
                  │   React 18 / Vite    │
                  └──────────┬───────────┘
                             │
                  HTTPS REST API Calls
                             │
                             ▼
                  ┌──────────────────────┐
                  │   Render (Backend)   │
                  │  Spring Boot (Java)  │
                  └──────┬────┬────┬─────┘
                         │    │    │
      ┌──────────────────┘    │    └────────────────┐
      │ JDBC (PostgreSQL)     │ SASL_SSL            │ Metrics Scraping
      ▼                       ▼                     ▼
┌───────────┐         ┌───────────────┐     ┌───────────────┐
│   Neon    │         │  Aiven Kafka  │     │ Grafana Cloud │
│ Database  │         │ Event Stream  │     │  Prometheus   │
└───────────┘         └───────────────┘     └───────────────┘
```

---

## 📋 Step-by-Step Deployment Steps

### Phase 1 — Database Deployment (Neon PostgreSQL)

1. Sign up on [https://neon.tech](https://neon.tech).
2. Click **Create Project** → Name it `ecom-db` → Select Region (e.g. `US East N. Virginia`).
3. Obtain the PostgreSQL connection details:
   - **Host**: `ep-xxxx.neon.tech`
   - **Database**: `neondb`
   - **Username**: `neondb_owner`
   - **Password**: *(your Neon password)*
4. Format the Java JDBC URL:
   `jdbc:postgresql://ep-xxxx.neon.tech/neondb?sslmode=require`

---

### Phase 2 — Managed Kafka Setup (Aiven Kafka Free Tier)

1. Sign up on [https://aiven.io](https://aiven.io).
2. Click **Create Service** → Select **Apache Kafka®** → Select **Free Tier ($0/mo)**.
3. Click **Topics** → Click **Add Topic** → Create topic named: `order-events` (1 partition).
4. Extract connection details from service **Overview**:
   - **Bootstrap Server**: `ecom-kafka-xxxx.aivencloud.com:28045`
   - **Username**: `avnadmin`
   - **Password**: *(Aiven user password)*
   - **Security Protocol**: `SASL_SSL`
   - **SASL Mechanism**: `SCRAM-SHA-256`

---

### Phase 3 — Backend Deployment (Render)

1. Sign up on [https://dashboard.render.com](https://dashboard.render.com) using GitHub.
2. Click **New +** → **Web Service** → Select repository `Full-Stack-E-Commerce-Application`.
3. Configure settings:
   - **Runtime**: `Docker`
   - **Root Directory**: `backend`
   - **Instance Type**: `Free`
4. Configure Environment Variables:

| Variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://ep-xxxx.neon.tech/neondb?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `neondb_owner` |
| `SPRING_DATASOURCE_PASSWORD` | *(your Neon password)* |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` |
| `JWT_SECRET` | *(strong base64 secret key)* |
| `KAFKA_ENABLED` | `true` |
| `KAFKA_BOOTSTRAP_SERVERS` | `ecom-kafka-xxxx.aivencloud.com:28045` |
| `KAFKA_SECURITY_PROTOCOL` | `SASL_SSL` |
| `KAFKA_SASL_MECHANISM` | `SCRAM-SHA-256` |
| `KAFKA_SASL_JAAS_CONFIG` | `org.apache.kafka.common.security.scram.ScramLoginModule required username="avnadmin" password="YOUR_PASSWORD";` |
| `SPRING_MAIL_HOST` | `smtp.gmail.com` |
| `SPRING_MAIL_PORT` | `587` |
| `SPRING_MAIL_USERNAME` | *(your email address)* |
| `SPRING_MAIL_PASSWORD` | *(your 16-character Google App Password)* |
| `RAZORPAY_API_KEY` | *(your Razorpay Key ID)* |
| `RAZORPAY_API_SECRET` | *(your Razorpay Secret)* |
| `CORS_ALLOWED_ORIGINS` | `https://<your-vercel-domain>.vercel.app,http://localhost:5173` |

5. Click **Create Web Service**.

---

### Phase 4 — Frontend Deployment (Vercel)

1. Sign up on [https://vercel.com](https://vercel.com) using GitHub.
2. Click **Add New...** → **Project** → Import `Full-Stack-E-Commerce-Application`.
3. Configure settings:
   - **Framework Preset**: `Vite`
   - **Root Directory**: `frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
4. Add Environment Variable:
   - `VITE_API_URL` = `https://<your-render-backend-url>.onrender.com/api`
5. Click **Deploy**.
6. Added [frontend/vercel.json](file:///c:/Desktop/Projects/Full%20Stack%20E-commerce%20Application/frontend/vercel.json) rewrite rule for client-side Vite SPA routing:
```json
{
  "rewrites": [
    { "source": "/(.*)", "destination": "/index.html" }
  ]
}
```

---

### Phase 5 — Observability (Grafana Cloud + Prometheus)

1. Sign up on [https://grafana.com](https://grafana.com).
2. Connect Prometheus to target: `https://<your-render-backend-url>.onrender.com/actuator/prometheus`.
3. Import Spring Boot Dashboard ID **`11378`**.

---

## 🔒 Security Best Practices Implemented

- ✅ **Zero Secrets in Code or Git:** `.env` and sensitive values are excluded from source code.
- ✅ **Separate Secret Isolation:** Render stores backend secrets; Vercel stores public frontend configuration only.
- ✅ **Encrypted Data Transit:** HTTPS, SSL Mode for Neon, SASL_SSL for Aiven Kafka.
- ✅ **Restricted CORS:** Accepting requests from authorized frontend domain only.
