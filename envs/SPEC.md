# Env Template Spec

## Context

An env defines how a bot will be run in the evaluator VM and provides a starter bot template.

An env is associated with a language.

An env has a slug that uniquely identifies it ().

## Env Template

An env is defined within a folder named by its slug

Within that folder:

### bot.edn

A bot.edn file, ex. for clojure-lein-uberjar:
```
{:env/slug "clojure-lein-uberjar"
 :env/language-slug "clojure"
 :env/enabled? true
 :env/runtime :runtime/firecracker
 :env/run-cmd "lein run"
 :env/build-cmd "lein uberjar"
 :env/artifact-path "target/bot-0.0.1-SNAPSHOT-standalone.jar"
 :env/argv ["java" "-jar" "$ARTIFACT"]}
 ```

```
 :env/slug
   string [a-z0-9-]
   the env slug

 :env/language-slug
   string [a-z0-9-]
   slug for the corresponding language

 :env/enabled?
   boolean
   whether this env is enabled in the prod system (default to false)

 :env/runtime
   keyword
   which runtime to use, default :runtime/firecracker

 :env/run-cmd
   string
   command to run program locally on the user's system (compiling beforehand if relevant)

 :env/build-cmd
   string or nil
   command to build an artifact on the user's system (targeting staticallyu linked x86 linux, when relevant; runtimes like the JVM or WASM have their own target). The artifact should be a single file (if necessary, zip).

 :env/artifact-path
   string
   path to where the build artifact is created

 :env/argv
   vector of strings
   args to run the build artifact in production
   use $ARTIFACT to represent the artifact file

 :env/note
   string or nil
   human-readable note for env users (e.g. setup prerequisites, platform-specific requirements); nil if none

 :env/status
   string or nil
   short status label displayed next to the env slug in CLI listings (e.g. "dev-only"); nil if none
```

### Starter Bot Files

Any other files will be instantiated for a user when they initialize a bot.

This must include a starter bot implementation that:
 - a "main" (or equivalent entry point function) that...
   - reads from stdin
   - parses stdin as json
   - passes the parsed input to a `run` function as an ~object
   - converts the output of the `run` function to json
   - outputs to stdout
 - the "run" function
   - takes an ~object as input
   - reads the value corresponding to the `ping` key from the input ~object
   - returns an ~object with a `pong` key associated with the ping value

