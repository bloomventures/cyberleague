
(defn rand-id []
  (string/join "" (take 20 (repeatedly #(rand-int 9)))))

(defn rand-name []
  (string/join "" (take 20 (repeatedly #(rand-nth ["a" "b" "c" "d" "e" "f" "g"])))))

(defn rand-words []
  (string/join " " (take 20 (repeatedly #(rand-nth ["four" "score" "seven" "years" "ago" "our" "founding" "fathers"])))))


(defn bot-name []
  (str (string/join "" (take 3 (repeatedly #(rand-nth "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))))
       "-"
       (+ 1000 (rand-int 8999))))

(defn gen-code []
  {:id (rand-id)
   :code "(fn [source] )"})

(defn gen-bot [user-id game-id]
  {:id (rand-id)
   :name (bot-name)
   :user-id user-id
   :game-id game-id
   :code-id code-id
   :code-version ""
   :rating (rand-int 1000)
   :rating-dev (rand-int 200)
   })

(defn gen-user []
  {:id (rand-id)
   :name (rand-name) })

(defn gen-match [bot-ids]
  {:id (gen-id)
   :bots bot-ids
   :moves []
   :first-move (first bot-ids)
   :winner (second bot-ids)})

(defn gen-game []
  {:id (rand-id)
   :name (rand-name)
   :description (rand-words)
   :rules (rand-words) })

(def user-james
  {:name "jamesnvc"
   :id 38405})

(def user-raf
  {:name "rafd"
   :id 89664})

(def game-goofspiel
  {:name "Goofspiel"
   :description "Also known as the Game of Perfect Strategy (GoPS), Goofspiel is a card game with simple rules but surprising depth.
                3 suits are taken from the game, one is yours to play with, another is your opponent's, and the third is the trophy deck (what you fight over). Each round, one of the trophy cards is revealed, then you and your opponent simultaneously bid one of your cards. The player with the higher bid score the value of the trophy card. If there's a tie, no one scores. The trophy and bid cards are removed. Once all trophy cards have been revealed, the scores are tallied: the winner is the player with the most points (sum of the value of the trophy cards they scored)."

   :rules "For our purposes, the game is played with the integers 1 through 12.

          ## Function Input:

          { :your-cards #{ 1 2 3 13 }
            :their-cards #{ 1 2 3 13 }
            :trophy-cards #{ 1 2 3 13 }
            :current-trophy 4
            :history [ { :you 1 :them 1 :trophy 1 } … ] } }

          ## Expected Output:

          a        ; a is the integer corresponding to your bid, it must be an integer that is still in your deck"

   })

(def game-ultimate-tic-tac-toe
  {:name "Ultimate Tic-Tac-Toe"
   :description "You mastered Tic-Tac-Toe in minutes, but let's see how long it will take you to master it's bigger brother.
                In Ultimate Tic-Tac-Toe, you play 9 games of Tic-Tac-Toe nested a meta Tic-Tac-Toe game."
   :rules "..."})

(def bot-goofspiel
  {:name (bot-name)
   :code (pr-str '(fn [source]
                    (let [trophy 1]
                      (get [2 3 4 5 6 7 8 9 10 11 12 1] (- trophy 1)))))})

(def bot-goofspiel-2
  {:name (bot-name)
   :code (pr-str '(fn [source]
                    (let [trophy 1]
                      (get [1 2 3 4 5 6 7 8 9 10 11 12] (- trophy 1)))))})

(def bot-goofspiel-3
  {:name (bot-name)
   :code (pr-str '(fn [source]
                    (let [trophy 1]
                      trophy)))})

(def bot-goofspiel-4
  {:name (bot-name)
   :code (pr-str '(fn [source]
                    (rand-nth my-cards)))})


