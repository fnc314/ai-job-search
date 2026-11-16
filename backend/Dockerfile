FROM eclipse-temurin:21-jre

ENV DEBIAN_FRONTEND=noninteractive

# Install lightweight TeX Live packages for headless remote PDF compilation
RUN apt-get update && apt-get install -y --no-install-recommends \
    texlive-latex-base \
    texlive-latex-extra \
    texlive-fonts-recommended \
    texlive-xetex \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY service/build/install/service /app

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["/app/bin/service"]
