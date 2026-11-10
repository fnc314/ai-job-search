FROM eclipse-temurin:21-jre

WORKDIR /app
COPY build/install/backend /app

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["/app/bin/backend"]
