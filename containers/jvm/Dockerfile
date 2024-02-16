# syntax=docker/dockerfile:1-labs

# base: ubuntu 22.04
FROM ubuntu@sha256:f9d633ff6640178c2d0525017174a688e2c1aef28f0a0130b26bd5554491f0da

ARG JAVA_VERSION=zulu21

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Cloud: JVM21 Base"
LABEL org.opencontainers.image.description="Java 21 tools for use with Elide Cloud apps"
LABEL org.opencontainers.image.version=alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/build-infra
LABEL org.opencontainers.image.base.name=elide-dev/runtime/jvm21
LABEL org.opencontainers.image.source=https://github.com/elide-dev/build-infra/blob/main/containers/jvm/Dockerfile

RUN --security=sandbox apt-get update \
  && apt-get -y upgrade \
  && groupadd builder \
  && useradd --system -g builder builder \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    ca-certificates \
    curl \
    git \
    gnupg \
    libc-dev \
  && apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0xB1998361219BD9C9 \
  && curl -O https://cdn.azul.com/zulu/bin/zulu-repo_1.0.0-3_all.deb \
  && apt-get install ./zulu-repo_1.0.0-3_all.deb \
  && rm -fv /zulu-repo_1.0.0-3_all.deb \
  && apt-get update \
  && apt-get install -y "${JAVA_VERSION}-jdk" wget tar \
  && rm -rf /var/lib/apt/lists/* \
  && echo "JDK image ready."

USER builder
ENV JAVA_HOME="/usr/lib/jvm/${JAVA_VERSION}" \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
