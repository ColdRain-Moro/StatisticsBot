FROM openjdk:11

RUN mkdir /app

COPY ./build/libs/StatisticsBot-1.0-SNAPSHOT-all.jar /app/StatisticsBot-1.0-SNAPSHOT-all.jar

COPY ./device.json /app/device.json

COPY ./update_device.py /app/update_device.py

COPY ./start.sh /app/start.sh

WORKDIR /app

RUN chmod +x start.sh

CMD ["bash", "./start.sh"]

EXPOSE 8080