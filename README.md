# Ketchup Waiter

A Discord bot for the Ketchup Discord Guild made in [Kotlin](https://kotlinlang.org/).

Manages events and clubs, allows users to create, manage and join them.
May over time get some additional features: Games, Integrations, etc.

Made possibly by [kordlid/kord](https://github.com/kordlib/kord) ❤️

## Features

In the Discord Guild, use `ketchup help` for available commands.

Here are the main ones:

- Create event channels
- Create club channels

You can also use `ketchup roadmap` for a list of planned features.

## Setup

GitHub has jar files attached to each release, you can download them there.
They target java version 21.
I run the bot on GraalVM 21, but it should work on any recent JVM. It uses low CPU and Memory.

The bot creates and uses a `storage.json` file in the working directory for configuration and persistence.
This may in future be used to configure different run modes for production and development.

### JVM

This is the main way to run the bot (other than from the IDE during development).

```bash
./gradlew fatjar --no-daemon
KETCHUP_BOT_TOKEN="" \
  KETCHUP_BOT_CLIENT_ID="" \
  java -jar build/libs/ketchup-bot-fatjar.jar
```

### Native Image

Native image should make the bot slightly faster, but more memory efficient (which would be nice).
But I haven't gotten this to run yet.

```bash
./gradlew fatjar --no-daemon
native-image -jar build/libs/ketchup-bot-fatjar.jar
KETCHUP_BOT_TOKEN="" \
  KETCHUP_BOT_CLIENT_ID="" \
  ./ketchup-bot-fatjar
```

### Docker

Builds an amd64 linux image from source in a multi-stage docker build.
Most reproducible way to build this project, last resort due to performance.

```bash
docker build . -t ketchup-bot:latest
docker run --rm -it \
  -e KETCHUP_BOT_TOKEN="" \
  -e KETCHUP_BOT_CLIENT_ID="" \
  ketchup-bot:latest
```

Run the container with:

```bash
docker run --rm -it \
  -e KETCHUP_BOT_TOKEN="" \
  -e KETCHUP_BOT_CLIENT_ID="" \
  mtibbecker/ketchup-bot:latest
```

## Release

This is just for @mtib to remember how to do this:

```bash
docker tag ketchup-bot:latest mtibbecker/ketchup-bot:latest
docker push mtibbecker/ketchup-bot:latest
```

```bash
./gradlew fatjar
scp build/libs/ketchup-bot-fatjar.jar mtib.dev:containers/ketchup-waiter/ketchup-bot-fatjar.jar 
# ssh mtib.dev; tmux at -t ketchup-bot; ^C; cd containers/ketchup-waiter; ./start.sh
```