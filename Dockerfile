FROM eclipse-temurin:17-jre

COPY target/uberjar/lupapiste-toj.jar /srv/app.jar

ADD config.edn /srv/config.edn
ADD ./entrypoint.sh /srv/entrypoint.sh

EXPOSE 8010
WORKDIR /srv

ENTRYPOINT ["/srv/entrypoint.sh"]
