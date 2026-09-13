# --- Építési fázis ---
FROM node:20-alpine AS build
WORKDIR /app

# Függőségek telepítése
COPY package*.json ./
RUN npm ci

# Forráskód másolása és buildelés
COPY . .
RUN npm run build

# --- Futtatási fázis ---
FROM node:20-alpine
WORKDIR /app

# Átmásoljuk a package fájlokat és a production függőségeket
COPY package*.json ./
RUN npm ci --production

# Átmásoljuk a buildelt mappát (SvelteKit node adapter alapértelmezetten a 'build' mappába dolgozik)
COPY --from=build /app/build ./build
COPY --from=build /app/package.json ./package.json

EXPOSE 3000
ENV PORT=3000
ENV HOST=0.0.0.0

CMD ["node", "build"]