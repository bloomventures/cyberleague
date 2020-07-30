(ns cyberleague.games.protocol)

(defprotocol IGameEngine
  (simultaneous-turns? [_] "Do the players of this game reveal moves simultaneous?")
  (number-of-players [_] "Return the number of players in the game")
  (anonymize-state-for [_ player-id state]
    "Return a copy of the state for the given user with other bot ids changed to anonymous identifiers")
  (valid-move? [_ move] "Is the player's move syntactically well-formed?")
  (init-state [_ players] "Create the initial state of the game")
  (legal-move? [_ state player move] "Is the player's move for the given game state legal?")
  (next-state [_ state moves] "Calculate the next state, given the current state
                              and an map of player moves like {player-1-id move1 player-2-id move2}")
  (game-over? [_ state] "Is the game in a finished state?")
  (winner [_ state] "If the game is over, return the winner of the game"))

(defmulti make-engine :game/name)
