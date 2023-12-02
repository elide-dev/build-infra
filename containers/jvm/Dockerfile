FROM ubuntu:22.04@sha256:90f3997e3e5f82097fa3046152944772876ac338365954f693b9e1bd3dd3c280

ARG JAVA_VERSION=zulu19

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Cloud: JVM19 Base"
LABEL org.opencontainers.image.description="Java 19 tools for use with Elide Cloud apps"
LABEL org.opencontainers.image.version=alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/build-infra
LABEL org.opencontainers.image.base.name=elide-dev/runtime/jvm19
LABEL org.opencontainers.image.source=https://github.com/elide-dev/build-infra/blob/main/containers/jvm/Dockerfile

RUN apt-get update \
  && apt-get -y upgrade \
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

ENV JAVA_HOME="/usr/lib/jvm/${JAVA_VERSION}" \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
