# syntax=docker/dockerfile:1-labs

ARG BASE_VERSION="@sha256:bbf3d1baa208b7649d1d0264ef7d522e1dc0deeeaaf6085bf8e4618867f03494"

FROM --platform=linux/amd64 ubuntu${BASE_VERSION}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide RBE"
LABEL org.opencontainers.image.description="Base image for remote build execution"

RUN --security=sandbox echo "Installing packages..." \
    && apt-get update \
    && apt-get -y upgrade \
    && apt-get install -y \
      wget \
      lsb-release \
      software-properties-common \
      gnupg \
      ca-certificates \
      curl \
      libc6 \
      zlib1g \
      libncurses5 \
    && apt-get autoremove \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN --security=sandbox echo "Installing JVM21..." \
    && wget -O jdk.deb "https://dl.less.build/toolchains/azul/zing/zing23.12.0.0-4-jdk21.0.1-linux_amd64.deb" \
    && dpkg -i jdk.deb \
    && rm -f jdk.deb \
    && ln -s /opt/zing/zing-jdk21 /usr/lib/jvm

RUN --security=sandbox echo "Installing clang..." \
    && wget https://apt.llvm.org/llvm.sh \
    && chmod +x llvm.sh \
    && ./llvm.sh 16 \
    && rm -f llvm.sh \
    && ln -s `which clang-16` /usr/bin/clang++ \
    && ln -s `which clang-16` /usr/bin/clang

RUN --security=sandbox echo "Installing Bazelisk..." \
    && wget -O bazelisk "https://github.com/bazelbuild/bazelisk/releases/download/v1.19.0/bazelisk-linux-amd64" \
    && mv bazelisk /bin/bazelisk \
    && chmod +x /bin/bazelisk \
    && chmod 555 /bin/bazelisk \
    && ln -s /bin/bazelisk /bin/bazel \
    && /bin/bazel version \
    && echo "Bazel is ready."

RUN echo "Preloading Bazel..." \
    && bazel version \
    && echo "Bazel RBE base image ready."

COPY bazel.rc /etc/bazel.bazelrc
ENV CC=clang \
    JAVA_HOME=/usr/lib/jvm \
    DEBIAN_FRONTEND=noninteractive
