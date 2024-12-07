# Auth

This document describes how to authenticate with the Ketchup HTTP API.

## Getting authenticated

Use the `/create token` command in the discord server. This will cause the bot to send you a DM with a token. Use this
bearer token in the `Authorization` header of your requests.

```http
Authorization: Bearer <token>;
```

This bearer token is tied to your discord account's snowflake. Do not share this token with anyone.

Note your discord snowflake id. It is required to call the API.

## Endpoints

These endpoints are also _documented_ in the [Bruno](https://www.usebruno.com/) collection
at [bruno/ketchup-api](../../bruno/ketchup-api). Remember to fill the environment with the required secrets to use it.

The base url for all endpoints in production is `https://api.ketchup.mtib.dev`. For development,
use `http://localhost:8505`.

Continue reading at:

- [Benchmarks](./aoc/Benchmark.md)
- [Inputs](./aoc/Input.md)

or browse the folder structure inside the [docs](./) folder.

### `POST /auth/check/:snowflake`

- Requires bearer token

Returns 200 and body is

```json
{
  "success": true
}
```

if authorization is correctly set up for user with snowflake `:snowflake`.
