(ns cyberleague.games.goofspiel.seed)

(def entities
  [{:game/name "goofspiel"
    :game/description (str "Also known as the Game of Perfect Strategy (GoPS), Goofspiel is a card game with simple rules but surprising depth.\n"
                           "3 suits are taken from the game, one is yours to play with, another is your opponent's, and the third is the trophy deck (what you fight over). Each round, one of the trophy cards is revealed, then you and your opponent simultaneously bid one of your cards. The player with the higher bid scores the value of the trophy card. If there's a tie, no one scores. The spent trophy and bid cards are removed, and the next round is played. Once all cards have been played, the scores are tallied: the winner is the player with the most points (the sum of the value of the trophy cards they scored).")
    :game/rules (str "For our purposes, the game is played with the integers 1 through 12.\n"
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
                     "a        ; a is the integer corresponding to your bid, it must be an integer that is still in your deck")}

   {:bot/user-name "jamesnvc"
    :bot/game-name "goofspiel"
    :bot/code (pr-str '(fn [state]
                         (let [trophy (state :current-trophy)]
                           (get [2 3 4 5 6 7 8 9 10 11 12 13 1] (dec trophy)))))}

   {:bot/user-name "rafd"
    :bot/game-name "goofspiel"
    :bot/code (pr-str '(fn [state]
                         (let [trophy (state :current-trophy)]
                           (get [1 2 3 4 5 6 7 8 9 10 11 12 13] (dec trophy)))))}

   {:bot/user-name "jamesnvc"
    :bot/game-name "goofspiel"
    :bot/code (pr-str '(fn [state]
                         (let [trophy (state :current-trophy)]
                           trophy)))}

   {:bot/user-name "rafd"
    :bot/game-name "goofspiel"
    :bot/code (pr-str '(fn [state]
                         (rand-nth (vec (get-in state [:player-cards :me])))))}])
