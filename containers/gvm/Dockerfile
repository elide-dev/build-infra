# syntax=docker/dockerfile:1-labs

# base: ubuntu 22.04
FROM ubuntu@sha256:2e863c44b718727c860746568e1d54afd13b2fa71b160f5cd9058fc436217b30

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Cloud: GVM21 Base"
LABEL org.opencontainers.image.description="GraalVM JDK21 tools for use with Elide Cloud apps"
LABEL org.opencontainers.image.version=alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/build-infra
LABEL org.opencontainers.image.base.name=elide-dev/runtime/gvm21
LABEL org.opencontainers.image.source=https://github.com/elide-dev/build-infra/blob/main/containers/gvm/Dockerfile

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
  && mkdir -p /tmp/gvm \
  && cd /tmp/gvm \
  && export ARCH="$(uname -m)" \
  && if [ "$ARCH" = "x86_64" ] ; then ARCH="x64"; fi \
  && if [ "$ARCH" = "arm64" ] ; then ARCH="aarch64"; fi \
  && export GRAAL_DOWNLOAD_URL="https://download.oracle.com/graalvm/21/latest/graalvm-jdk-21_linux-${ARCH}_bin.tar.gz" \
  && echo "GraalVM Download URL: $GRAAL_DOWNLOAD_URL" \
  && curl --progress-bar -SL "$GRAAL_DOWNLOAD_URL" > ./graalvm.tar.gz \
  && tar -xzvf ./graalvm.tar.gz \
  && mv "./graalvm-jdk-21.0.2+13.1" /usr/lib/gvm \
  && rm -f ./graalvm.tar.gz \
  && cd / \
  && rm -fr /tmp/gvm \
  && rm -rf /var/lib/apt/lists/*

USER builder

ENV GRAALVM_HOME=/usr/lib/gvm \
    PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin
