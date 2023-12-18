# Ketchup Bot

## Setup

### JVM

```bash
./gradlew fatjar --no-daemon
KETCHUP_BOT_TOKEN="" \
  KETCHUP_BOT_CLIENT_ID="" \
  java -jar build/libs/ketchup-bot-fatjar.jar
```

### Docker

```bash
docker build . -t ketchup-bot:latest
docker run --rm -it \
  -e KETCHUP_BOT_TOKEN="" \
  -e KETCHUP_BOT_CLIENT_ID="" \
  ketchup-bot:latest
```