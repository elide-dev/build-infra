# syntax=docker/dockerfile:1-labs

# mantic
ARG BASE_VERSION="@sha256:8d093e0651575a6437cc4a3d561f892a345d263aeac6156ef378fe6a4ccabd4c"

FROM --platform=linux/amd64 ubuntu${BASE_VERSION}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide RBE"
LABEL org.opencontainers.image.description="Base image for remote build execution"

RUN --security=sandbox echo "Installing packages..." \
    && DEBIAN_FRONTEND=noninteractive apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y \
      wget \
      lsb-release \
      software-properties-common \
      gnupg \
      ca-certificates \
      curl \
      libc6 \
      zlib1g \
    && echo "Installing clang..." \
    && wget --max-redirect 0 https://apt.llvm.org/llvm.sh \
    && chmod +x llvm.sh \
    && ./llvm.sh 16 \
    && rm -f llvm.sh \
    && ln -s `which clang-16` /usr/bin/clang++ \
    && ln -s `which clang-16` /usr/bin/clang \
    && echo "Installing packages..." \
    && DEBIAN_FRONTEND=noninteractive apt-get -y upgrade \
    && DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y \
      libncurses5 \
      tclsh \
      build-essential \
      linux-headers-$(uname -r) \
      libc++-dev \
      llvm-16 \
    && update-alternatives --install /usr/bin/llvm-config llvm-config /usr/bin/llvm-config-16.0.0 200 \
    && apt-get autoremove \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && echo "Installing JVM21..." \
    && wget -O jdk.deb "https://dl.less.build/toolchains/azul/zing/zing23.12.0.0-4-jdk21.0.1-linux_amd64.deb" \
    && dpkg -i jdk.deb \
    && rm -f jdk.deb \
    && ln -s /opt/zing/zing-jdk21 /usr/lib/jvm
    && echo "Installing Bazelisk..." \
    && wget -O bazelisk "https://github.com/bazelbuild/bazelisk/releases/download/v1.19.0/bazelisk-linux-amd64" \
    && mv bazelisk /bin/bazelisk \
    && chmod +x /bin/bazelisk \
    && chmod 555 /bin/bazelisk \
    && ln -s /bin/bazelisk /bin/bazel \
    && /bin/bazel version \
    && echo "Bazel is ready."
    && echo "Preparing user..." \
    && groupadd bazel \
    && useradd --system -m -g bazel bazel \
    && chown bazel:bazel -R /home/bazel \
    && mkdir -p /bazel/workdir
    && echo "Preloading Bazel..." \
    && bazel version \
    && echo "Bazel RBE base image ready."

COPY bazel.rc /etc/bazel.bazelrc

ENV CC=clang \
    JAVA_HOME=/usr/lib/jvm \
    DEBIAN_FRONTEND=noninteractive
