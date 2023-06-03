FROM ghcr.io/elide-dev/build-infra/gvm:latest

ARG NODE_VERSION=20
ARG YARN_VERSION="berry"

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        ca-certificates \
        build-essential \
        mysql-client \
        redis-tools \
        golang \
    && curl -sL "https://deb.nodesource.com/setup_$NODE_VERSION.x" | bash - \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs \
    && npm install -g \
        "yarn@$YARN_VERSION" \
        pnpm \
    && rm -rf /var/lib/apt/lists/*
