# Multi-stage build: install, build, then run only the standalone server (no dev dependencies).

FROM node:24-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:24-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# Frozen into the static pages at build time (SEO URLs, which house to show): one image per site.
ARG NEXT_PUBLIC_SITE_URL=http://localhost:3000
ARG NEXT_PUBLIC_PROPERTY_ID=6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b
ENV NEXT_PUBLIC_SITE_URL=$NEXT_PUBLIC_SITE_URL \
    NEXT_PUBLIC_PROPERTY_ID=$NEXT_PUBLIC_PROPERTY_ID \
    NEXT_TELEMETRY_DISABLED=1
RUN npm run build

FROM node:24-alpine AS run
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0 \
    API_URL=http://api:8080
# The node image ships an unprivileged "node" user.
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
COPY --from=build --chown=node:node /app/public ./public
USER node
EXPOSE 3000
HEALTHCHECK --interval=15s --timeout=3s --start-period=20s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:3000/en || exit 1
CMD ["node", "server.js"]
