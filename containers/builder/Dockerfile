# syntax=docker/dockerfile:1-labs
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
    && go install github.com/google/gnostic/cmd/protoc-gen-openapi@latest && \
      ln -s $(go env GOPATH)/bin/protoc-gen-openapi /usr/local/bin/protoc-gen-openapi \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs \
    && npm install -g \
        "yarn@$YARN_VERSION" \
        pnpm \
    && groupadd builder \
    && useradd --system -g builder builder \
    && rm -rf /var/lib/apt/lists/*

USER builder
