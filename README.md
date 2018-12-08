# branch-watcher

A small utility to help you keep your git branches tidy.

## Building

```sh
lein uberjar
```

## Usage

You will need to create a [Personal Access Token][0] and export it as an
environment variable.

[0]: https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/

```sh
export GITHUB_TOKEN=<token>
java -jar target/uberjar/branch-watcher-0.1.0-SNAPSHOT-standalone.jar <org> <repo>
```

## Examples

```sh
$ java -jar target/uberjar/branch-watcher-0.1.0-SNAPSHOT-standalone.jar alpinelinux aports

|           :author |       :name |                :date |
|-------------------+-------------+----------------------|
|    Leonardo Arena |         1.9 | 2010-04-09T09:40:34Z |
|    Leonardo Arena |  2.4-stable | 2014-09-27T10:02:12Z |
|    Leonardo Arena |  2.6-stable | 2015-09-14T08:21:50Z |
|    Leonardo Arena |  3.4-stable | 2018-06-11T13:00:50Z |
|    Leonardo Arena |  3.5-stable | 2018-06-11T13:14:41Z |
|    Leonardo Arena |  3.6-stable | 2018-06-11T13:14:41Z |
|     Natanael Copa |  2.1-stable | 2013-01-17T15:18:59Z |
|     Natanael Copa |  2.3-stable | 2013-10-09T06:10:04Z |
|     Natanael Copa |  2.5-stable | 2014-10-23T09:28:52Z |
|     Natanael Copa |  3.2-stable | 2015-12-15T15:06:57Z |
|     Natanael Copa |      master | 2018-06-12T20:41:00Z |
```

## Concourse Pipeline

```sh
fly --target=<target> set-pipeline \
    --config=pipeline.yml \
    --pipeline=branch-watcher \
    --var=github_token="${GITHUB_TOKEN}"
```

## License

Copyright © 2018 Bradford D. Boyle

Distributed under the Apache License Version 2.0.
