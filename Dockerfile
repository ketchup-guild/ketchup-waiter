FROM ghcr.io/graalvm/jdk-community:21 AS build

WORKDIR /app
COPY . .

RUN ./gradlew --no-daemon fatjar

FROM ghcr.io/graalvm/jdk-community:21 AS runtime

COPY --from=build /app/build/libs/ /app/

ENV KETCHUP_BOT_TOKEN=""
ENV KETCHUP_BOT_CLIENT_ID=""

WORKDIR /app

CMD ["java", "-jar", "./ketchup-bot-fatjar.jar"]