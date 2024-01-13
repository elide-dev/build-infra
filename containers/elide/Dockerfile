# syntax=docker/dockerfile:1-labs

# base=elide alpha v7
FROM ghcr.io/elide-dev/elide@sha256:2a7cbc25ecb484aa1f41222a50d621e61527cdfc4d5c7d8131bfd42dba689974

RUN --security=sandbox groupadd builder && useradd --system -g builder builder
USER builder
