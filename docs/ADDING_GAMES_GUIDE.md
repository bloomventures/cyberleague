# Adding Games

Each game is defined by two parts: a config map registered via `game-registrar/register-game!`, and an engine implementing the `IGameEngine` protocol.

### Config map (`:game.config/*`)

- `:game.config/name` — display name (e.g. `"goofspiel"`)
- `:game.config/slug` — URL-safe identifier (e.g. `"ultimate-tic-tac-toe"`)
- `:game.config/description` — brief human-readable description of the game (markdown)
- `:game.config/rules` — rules of the game, typically as a bulleted list (markdown)
- `:game.config/technical-notes` - clarifications of rules for how they were converted to digital play
- `:game.config/context-spec` — malli spec for the context passed to bots as input
- `:game.config/context-example` — example context value
- `:game.config/move-spec` — malli spec for a valid move
- `:game.config/move-example` — example move value
- `:game.config/state-spec` — malli spec for the full internal game state
- `:game.config/match-results-view` — hiccup/reagent component to render match results
- `:game.config/test-bot` — bot used for basic upload validation
- `:game.config/seed-bots` — map of bot blueprints seeded for testing `{:blueprint/env-slug "clojure-sci" :blueprint/code (pr-str '(fn [context] ,,,))}`

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


