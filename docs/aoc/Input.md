# Input

## Getting authenticated

Refer to the [Auth](../Auth.md) documentation for how to get authenticated.

## Endpoints

These endpoints are also _documented_ in the [Bruno](https://www.usebruno.com/) collection
at [bruno/ketchup-api](../../bruno/ketchup-api). Remember to fill the environment with the required secrets to use it.

The base url for all endpoints in production is `https://api.ketchup.mtib.dev`. For development,
use `http://localhost:8505`.

## `GET /aoc/input/:snowflake/:year/:day`

- Requires bearer token

Currently only allows you to retrieve your own input.

Returns 200 and the user's input data as the response body.

## `POST /aoc/input/:snowflake/:year/:day`

- Requires bearer token

POST with the input data as the request body.

Returns 200 and

```json5
{
  "message": "ok"
}
```
