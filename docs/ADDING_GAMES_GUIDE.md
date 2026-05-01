# Adding Games

Each game is defined by two parts: a config map registered via `game-registrar/register-game!`, and an engine implementing the `IGameEngine` protocol.

### Config map (`:game.config/*`)

- `:game.config/name` — display name (e.g. `"goofspiel"`)
- `:game.config/slug` — URL-safe identifier (e.g. `"ultimate-tic-tac-toe"`)
- `:game.config/description` — human-readable description shown in the UI
- `:game.config/rules` — detailed rules / bot interface docs (input/output format)
- `:game.config/move-spec` — malli spec for a valid move
- `:game.config/move-example` — example move value
- `:game.config/public-state-spec` — malli spec for the state passed to bots
- `:game.config/public-state-example` — example public state value
- `:game.config/internal-state-spec` — malli spec for the full internal game state
- `:game.config/match-results-view` — hiccup/reagent component to render match results
- `:game.config/test-bot` — bot used for basic upload validation
- `:game.config/seed-bots` — list of `{:code/language :code/code}` bots seeded for matchmaking

### Engine (`IGameEngine` protocol)

Each game implements `cyberleague.games.protocol/IGameEngine`:

- `simultaneous-turns?` — whether both players reveal moves at the same time (e.g. goofspiel: yes; othello: no)
- `number-of-players` — player count (currently all games are 2-player)
- `init-state [players]` — create the initial game state
- `anonymize-state-for [player-id state]` — strip or anonymize opponent identity before sending state to a bot
- `valid-move? [move]` — syntactic well-formedness check (matches move-spec)
- `legal-move? [state player move]` — legality check given current game state
- `next-state [state moves]` — advance state given a map of `{player-id move}`
- `game-over? [state]` — is the game finished?
- `winner [state]` — return the winning player-id (or nil for draw)

Engines are registered via `defmethod protocol/make-engine :game/name`.


