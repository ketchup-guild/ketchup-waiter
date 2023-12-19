# Ketchup Bot

## Setup

### JVM

```bash
./gradlew fatjar --no-daemon
KETCHUP_BOT_TOKEN="" \
  KETCHUP_BOT_CLIENT_ID="" \
  java -jar build/libs/ketchup-bot-fatjar.jar
```

### Native Image

```bash
./gradlew fatjar --no-daemon
native-image -jar build/libs/ketchup-bot-fatjar.jar
KETCHUP_BOT_TOKEN="" \
  KETCHUP_BOT_CLIENT_ID="" \
  ./ketchup-bot-fatjar
```

### Docker

```bash
docker build . -t ketchup-bot:latest
docker run --rm -it \
  -e KETCHUP_BOT_TOKEN="" \
  -e KETCHUP_BOT_CLIENT_ID="" \
  ketchup-bot:latest
```

```bash
docker run --rm -it \
  -e KETCHUP_BOT_TOKEN="" \
  -e KETCHUP_BOT_CLIENT_ID="" \
  mtibbecker/ketchup-bot:latest
```

## Release

```bash
docker tag ketchup-bot:latest mtibbecker/ketchup-bot:latest
docker push mtibbecker/ketchup-bot:latest
```