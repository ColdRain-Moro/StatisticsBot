./gradlew build
docker build -t arcticrain/statistics-bot --platform linux/amd64 .
docker push arcticrain/statistics-bot