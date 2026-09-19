# --- Építési fázis ---
FROM node:20-alpine AS build
WORKDIR /app

# Függőségek telepítése
COPY package*.json ./
RUN npm install --legacy-peer-deps

# Forráskód másolása és buildelés
COPY . .

# Itt kapja meg a Docker a SvelteKit számára élesítéshez szükséges változót
ARG PUBLIC_API_URL
ENV PUBLIC_API_URL=$PUBLIC_API_URL

RUN npm run build

# --- Futtatási fázis ---
FROM node:20-alpine
WORKDIR /app

# Átmásoljuk a package fájlokat és a production függőségeket (legacy-peer-deps-szel)
COPY package*.json ./
RUN npm install --omit=dev --legacy-peer-deps

# Átmásoljuk a buildelt mappát (SvelteKit node adapter alapértelmezetten a 'build' mappába dolgozik)
COPY --from=build /app/build ./build
COPY --from=build /app/static ./static
COPY --from=build /app/package.json ./package.json

EXPOSE 3000
ENV PORT=3000
ENV HOST=0.0.0.0

CMD ["node", "build"]