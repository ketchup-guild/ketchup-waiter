# Benchmark

## Getting authenticated

Refer to the [Auth](../Auth.md) documentation for how to get authenticated.

## Endpoints

These endpoints are also _documented_ in the [Bruno](https://www.usebruno.com/) collection
at [bruno/ketchup-api](../../bruno/ketchup-api). Remember to fill the environment with the required secrets to use it.

The base url for all endpoints in production is `https://api.ketchup.mtib.dev`. For development,
use `http://localhost:8505`.

### `GET /aoc/benchmark/user/:snowflake`

- Requires bearer token

Returns 200 and the user's benchmark data in the following format:

```json5
{
  "benchmarks": {
    /* year */
    "2024": {
      /* day */
      "1": {
        /* part */
        "1": [
          /* list of benchmarks for year, day, part in order of submission */
          {
            "user_snowflake": "<your snowflake>",
            "event": "2024",
            "day": 1,
            "part": 1,
            "time_ms": 1234.56,
            "timestamp": 1733030269.4653163
          },
          /* ... */
        ],
        "2": [
          /* ... */
        ]
      }
      /* ... */
    }
    /* ... */
  },
  "user": "<your snowflake>",
  "timestamp_millis": 1733434612130,
  "query_time_micros": 148734
}
```

Note: arrays and objects are omitted if they are empty.

### `POST /aoc/benchmark/user/:snowflake`

- Requires bearer token

POST with json body:

```json
{
  "year": 2024,
  "day": 3,
  "part": 1,
  "time_ms": 1234.56
}
```

Returns 200, creates a benchmark entry for your user and body is

```json5
{
  "message": "ok",
  "data": {
    "year": 2024,
    "day": 1,
    "part": 1,
    "time_ms": 1234.56
  },
  /* epoch seconds timestamp of your submission */
  "timestamp_epoch_seconds": 1733030269,
  /* epoch seconds timestamp of the puzzle releasing */
  "puzzle_release_epoch_seconds": 1733030000,
  /* human-readable string of duration between release and submission */
  "time_since_puzzle_release": "27m 4s"
}
```

### `DELETE /aoc/benchmark/user/:snowflake/:epochSeconds`

- Requires bearer token

Deletes all benchmark entries for the user with snowflake `:snowflake` and timestamp `:epochSeconds` (higher precision
timestamps are rounded down). So to delete a benchmark with timestamp `1733030269.4653163` you would use `1733030269`.

Returns 200, creates a benchmark entry for your user and body is

```json5
{
  "message": "ok",
  /* number of entries deleted */
  "deleted": 1
}
```

### `POST /aoc/announce/:channelsnowflake/:snowflake/:day`

- Requires bearer token

POST with text body to create a message in the thread for the day `:day` in the channel `:channelsnowflake`. The body of
the request will be included in the message.

```json5
{
  "message": "ok",
  /* reflects :day */
  "day": 3
}
```

### `POST /auth/check/:snowflake`

- Requires bearer token

Returns 200 and body is

```json
{
  "success": true
}
```

if authorization is correctly set up for user with snowflake `:snowflake`.