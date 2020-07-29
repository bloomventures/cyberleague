; Code a bot to play Goofspiel
;
; The game is played with 3 'decks' of 1 through 13.
; Each turn, a trophy card is revealed and you
; and your opponent bid with one of your cards.
; The highest bid scores the value of the trophy card.
; Highest score after 13 cards wins.
;
; Sample Input:
;    { :player-cards {:me #{ 1 2 3 13 }
;                     :opponent #{ 1 2 3 13 }}
;      :trophy-cards #{ 1 2 3 13 }
;      :current-trophy 4
;      :history [ { :me 1 :opponent 1 :trophy 1 } … ] }
;
; Sample Output:
;   5
;
; When you're ready to test your code, hit 'Test'.
; Your bot will be put up against a random opponent.
; (println) and error output is shown on the right.
; Fix any bugs, and when you're happy, hit 'Deploy'.

(fn [state]
  (state :current-trophy))
