FROM node:22-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Run stage
FROM nginx:alpine

# Use an unprivileged non-root user setup for nginx if desired, 
# or standard nginx alpine image. We will stick to standard for simplicity.

COPY --from=builder /app/dist /usr/share/nginx/html

# SPA fallback configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
