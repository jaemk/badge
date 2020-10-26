# badge

**NOTE**: Development has been moved back to https://github.com/jaemk/badge-cache

> shields.io caching service

## Build/Installation

Build from source or see [releases](https://github.com/jaemk/badge/releases)
for pre-built executables (jre is still required)

```
# generate a standalone jar wrapped in an executable script
$ lein bin
```

## Usage

```
# start the server
$ export PORT=3003        # default
$ export REPL_PORT=3999   # default
$ export INSTRUMENT=false # disable spec assertions
$ bin/badge

# connect to running application
$ lein repl :connect 3999
user=> (initenv)  ; loads a bunch of namespaes
user=> (cmd/purge-files)  ; delete all cached files
```

## Testing

```
# run test
$ lein midje

# or interactively in the repl
$ lein with-profile +dev repl
user=> (autotest)
```

## Docker

```
# build
$ docker build -t badge:latest .
# run
$ docker run --rm -p 3003:3003 -p 3999:3999 --env-file .env.values badge:latest
```

[Images](https://hub.docker.com/r/jaemk/badge/tags)


## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
