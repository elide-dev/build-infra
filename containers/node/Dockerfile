# syntax=docker/dockerfile:1-labs
FROM node:21@sha256:ca1e0e5a87da404c488223e35dc7203c331e4c49c583616cc75a86ec13a43f3c

ARG YARN_VERSION=berry
RUN --security=sandbox npm install -g --ignore-scripts \
    npm@9.6.7 \
    pnpm \
    && groupadd builder \
    && useradd --system -g builder builder

USER builder
