FROM openjdk:11-jdk

RUN apt-get update && \
    apt-get install -y sqlite3 netcat

COPY . /app
WORKDIR /app

COPY wait /bin/wait-for-ports

CMD wait-for-ports /app/gradlew build && /app/gradlew run