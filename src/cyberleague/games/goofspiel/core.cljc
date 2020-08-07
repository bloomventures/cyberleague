(ns cyberleague.games.goofspiel.core
  (:require
   [cyberleague.game-registrar]
   [cyberleague.games.goofspiel.engine] ;; so it gets registered
   [cyberleague.games.goofspiel.bots :as bots]
   [cyberleague.games.goofspiel.ui :as ui]
   [cyberleague.games.goofspiel.starter-code :as starter-code]))

(cyberleague.game-registrar/register-game!
 {:game.config/name "goofspiel"
  :game.config/description
  (str "Also known as the Game of Perfect Strategy (GoPS), Goofspiel is a card game with simple rules but surprising depth.\n"
       "3 suits are taken from the game, one is yours to play with, another is your opponent's, and the third is the trophy deck (what you fight over). Each round, one of the trophy cards is revealed, then you and your opponent simultaneously bid one of your cards. The player with the higher bid scores the value of the trophy card. If there's a tie, no one scores. The spent trophy and bid cards are removed, and the next round is played. Once all cards have been played, the scores are tallied: the winner is the player with the most points (the sum of the value of the trophy cards they scored).")
  :game.config/rules
  (str "For our purposes, the game is played with the integers 1 through 12.\n"
       "## Function Input:\n"
       "\n"
       "{:your-cards #{ 1 2 3 13 }
        :their-cards #{ 1 2 3 13 }
        :trophy-cards #{ 1 2 3 13 }
        :current-trophy 4
        :history [ { :you 1 :them 1 :trophy 1 } … ] } }\n"
       "\n"
       "## Expected Output:\n"
       "\n"
       "a        ; a is the integer corresponding to your bid, it must be an integer that is still in your deck")
  :game.config/match-results-view ui/match-results-view
  :game.config/match-results-styles ui/>results-styles
  :game.config/starter-code starter-code/starter-code
  :game.config/test-bot (pr-str bots/random-bot)
  :game.config/seed-bots [(pr-str bots/random-bot)
                          (pr-str bots/current-trophy-bot)
                          (pr-str bots/other-bot)]})
