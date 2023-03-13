FROM openjdk:11

RUN mkdir /app

COPY ./build/libs/StatisticsBot-1.0-SNAPSHOT-all.jar /app/StatisticsBot-1.0-SNAPSHOT-all.jar

COPY ./device.json /app/device.json

WORKDIR /app

CMD ["java", "-jar", "StatisticsBot-1.0-SNAPSHOT-all.jar"]

EXPOSE 8080