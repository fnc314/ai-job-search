FROM eclipse-temurin:21-jre

WORKDIR /app
COPY service/build/install/service /app

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["/app/bin/service"]
