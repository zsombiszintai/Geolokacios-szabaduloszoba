ARG KEYCLOAK_VERSION=26.5.3

# A téma lefordítása
FROM node:22-bookworm-slim AS theme-build

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       openjdk-17-jdk-headless maven \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build-keycloak-theme

# Keycloak a saját témával
FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION}

COPY --from=theme-build --chown=keycloak:keycloak \
    /app/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar \
    /opt/keycloak/providers/cityscape-theme.jar

RUN /opt/keycloak/bin/kc.sh build --db=postgres