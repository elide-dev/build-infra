# syntax=docker/dockerfile:1-labs

# base: ubuntu 22.04
FROM ubuntu@sha256:6042500cf4b44023ea1894effe7890666b0c5c7871ed83a97c36c76ae560bb9b

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
  && useradd --system --group builder \
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
