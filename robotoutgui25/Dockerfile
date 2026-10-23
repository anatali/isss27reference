## ----------------------------------------------------------
## Dockerfile for unibo.robotoutgui25
## ----------------------------------------------------------

FROM eclipse-temurin:17.0.5_8-jre-focal as builder
## EXPOSE 8085
## ADD extracts the tar
ADD ./build/distributions/robotoutgui25-1.0.tar /
WORKDIR /robotoutgui25-1.0/bin
COPY ./*.pl ./
COPY ./*.json ./
COPY ./*.bin ./
COPY ./*.txt ./

## RUN apt-get update -y
## RUN apt-get install -y wiringpi
## RUN sudo apt-get install -y python

CMD ["bash", "robotoutgui25"]

## 0) docker rmi robotoutgui25:1.0
## 1) gradlew distTar
## 2) docker build -t robotoutgui25:1.0 .
 
## docker run -it --rm --name robotoutgui25 -p8085:8085/tcp   --privileged robotoutgui25:1.0  /bin/bash
## MODIFICA DEL FILE DI CONFIGURAZIONE
# docker cp basicrobotConfig.json 89193dba02a2:/robotoutgui25-1.0/bin/basicrobotConfig.json
# docker cp stepTimeConfig.json cab3aa29f8eb:/robotoutgui25-1.0/bin/stepTimeConfig.json

# docker tag robotoutgui25:1.0 natbodocker/robotoutgui25:1.0

#type docker_password.txt | docker login --username natbodocker --password-stdin
#docker login --username natbodocker --password xyz
# docker push natbodocker/robotoutgui25:1.0


# docker-compose -f robotoutgui25.yaml  up