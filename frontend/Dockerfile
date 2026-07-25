# ---------- Dépendances ----------
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json ./
# Pas de lockfile pour l'instant : npm install (et non npm ci)
RUN npm install --no-audit --no-fund

# ---------- Build ----------
FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# ---------- Exécution (Next.js standalone) ----------
FROM node:22-alpine AS run
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
ENV HOSTNAME=0.0.0.0
ENV PORT=3000
EXPOSE 3000
USER node
CMD ["node", "server.js"]
