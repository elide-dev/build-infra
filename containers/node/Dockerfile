# syntax=docker/dockerfile:1-labs
FROM node:21@sha256:cabbf4a9467b3f0d4ac4dc4e1b36d41f6ce83bed9d2c91c9715622f5c898dfb6

ARG YARN_VERSION=berry
RUN --security=sandbox npm install -g --ignore-scripts \
    npm@9.6.7 \
    pnpm \
    && useradd --system --group builder

USER builder
