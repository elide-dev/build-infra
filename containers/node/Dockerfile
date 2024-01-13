# syntax=docker/dockerfile:1-labs

# base=node 21
FROM cgr.dev/chainguard/node@sha256:f067a2587f6af9de292ffada8d633d664ec6391c733a7e75ca2c385294d7cebc

ARG YARN_VERSION=berry
RUN --security=sandbox npm install -g --ignore-scripts \
    yarn \
    pnpm \
    && groupadd builder \
    && useradd --system -g builder builder

USER builder
