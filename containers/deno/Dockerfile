# syntax=docker/dockerfile:1-labs

# base=deno 1.37
FROM cgr.dev/chainguard/deno@sha256:df9292f589e916105ff925527a3588c917e464953a056b6ad980e58bb1757aec

RUN --security=sandbox groupadd builder && useradd --system -g builder builder
USER builder
