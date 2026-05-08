# Bot Development Guide

The overall worklflow is:

- download the cli (once)
- authenticate the cli (once)
- create a bot
- work on the bot (locally)
- build the bot (locally)
- upload the bot
- test the bot (remotely, by being played against a dummy bot)
- view test match results (on the website)
- deploy a version of the bot into the tournament (one version of a bot can be active at a time)
- wait for the bot to play games against other active bots
- view match results (on the website)
- iterate

## Getting Started

- log into the website (via github oauth)
- download the cli tool:
  - apple silicon mac: TODO
  - linux x86: TODO
  - [jar download](https://github.com/bloomventures/cyberleague/releases/):
    - for this option, you need java installed
    - you will run the CLI via: `java -jar cyberleague.jar`
- authenticate the cli (`java -jar cyberleague.jar login`)
  - (copy the CLI token from your profile card on the [website](https://cyberleague.rafd.me/))
  - (this creates `cyberleague.conf.edn` in your current directory)
  - (the CLI will look for `cyberleague.conf.edn` in the current directory and ancestors)
- choose a [game](https://github.com/bloomventures/cyberleague/tree/master/src/cyberleague/games) and [env](https://github.com/bloomventures/cyberleague/tree/master/envs) for your bot
  - to list available games, run: `java -jar cyberleague.jar games`
  - to list available envs, run: `java -jar cyberleague.jar envs`
- create a new bot, ex. `java -jar cyberleague.jar bot new --game goofspiel --env clojure`
  - this creates:
      ```
      ./goofspiel-xyz
         bot.edn
           {:bot/id #uuid "..."
            :bot/name "xyz"
            :bot/game "goofspiel"
            :bot/env "clojure"
            :bot/run-cmd "lein run"
            :bot/build-cmd "lein uberjar"
            :bot/build-artifact "target/bot.jar"}
         other files for a starter bot based on the env chosen
      ```
  - you can modify the `run-cmd`, `build-cmd`, and `build-artifact` to use your preferred tools
  - you can build, upload, and test your bot via the stage command
    - `java -jar cyberleague.jar bot stage --dir goofspiel-xyz`
    - (or, `cd` into your bot directory, and run `java -jar ../cyberleague.jar bot stage`)
    - the starter code implements the ping-pong handshake that all bots must implement, but nothing else
    - the test involves running a match between your bot and a dummy bot
    - you can view the test match results on the website (navigate to your bot card)
    - the match should indicate a passed handshake - you are ready to work on your bot!


## Bot Basics

- every bot receives a game context as STDIN and must return its move on STDOUT
  - STDIN is JSON of the game context (the schema of which varies by game)
  - STDOUT must be JSON of the move your bot does (the schema of which varies by game)
  - to see the game rules and schemas, view the game on the website
- a bot must be stateless (re-calculating its move just based off of the game context it receives)
  - (the context the bot receives will include a history of moves)
- you can log to STDERR, which will be exposed in the match results
- you can use third-party libraries in your bot build

Some important limits to be aware of:

- your bot has a 1 second time limit to calculate a move
- your bot has no network access
- your bot has no disk access
- all bots must result in a single file artifact
  - (for some languages, this requires a zip step)
- your build artifact is limited to 50mb in size

## Workflow

- the typical workflow is to:
  - work on your bot locally
  - run `java -jar ../cyberleague.jar bot stage` to build, upload and test your bot
  - view the test match results on the website
  - when ready, "deploy" the current version of the bot via: `java -jar ../cyberleague.jar bot deploy`

## Additional Commands

- `java -jar ../cyberleague.jar bot build`
  - run the build step on its own
- `java -jar ../cyberleague.jar bot upload`
  - run the upload step on its own (with the current artifact)
- `java -jar ../cyberleague.jar bot test`
  - run the test step on its own (with the current artifact)
