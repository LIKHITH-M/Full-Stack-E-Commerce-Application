# =============================================================
# Stage 1: Build - Node 20 Alpine
# Runs: npm ci + npm run build -> produces dist/
# =============================================================
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files first (same layer caching trick as backend pom.xml).
# npm ci installs exact versions from package-lock.json.
# If package.json/lock don't change, this ~200 MB layer is reused.
COPY package.json package-lock.json ./
RUN npm ci

# Copy source and build.
# VITE_API_URL is set to empty here - the nginx proxy handles /api/* routing
# so the frontend never needs a hardcoded backend URL.
# The fallback in axios.jsx (||  "http://localhost:8081/api") is ignored
# when VITE_API_URL is set, even if set to "" - see nginx.conf proxy_pass.
COPY . .
RUN VITE_API_URL=/api npm run build

# =============================================================
# Stage 2: Runtime - Nginx Alpine (tiny, ~25 MB final image)
# =============================================================
FROM nginx:alpine

# Remove the default Nginx config and replace with our custom SPA config
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy the Vite production build output from stage 1
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

# Nginx runs in the foreground (daemon off) so Docker tracks the process
CMD ["nginx", "-g", "daemon off;"]
