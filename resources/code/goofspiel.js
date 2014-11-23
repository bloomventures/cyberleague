/*
 Code a bot to play Goofspiel

 The game is played with 3 'decks' of 1 through 13.
 Each turn, a trophy card is revealed and you
 and your opponent bid with one of your cards.
 The highest bid scores the value of the trophy card.
 Highest score after 13 cards wins.

 Sample Input:
    { "player-cards": {"me": [ 1, 2, 3, 13 ],
                       "opponent": [ 1, 2, 3, 13 ]}
      "trophy-cards": [ 1, 2, 3 ],
      "current-trophy": 4,
      "history":  [ { "me": 4,
                      "opponent": 5,
                      "trophy": 5 },
                    { "me": 7,
                      "opponent": 8,
                      "trophy": 9 }, … ] }

 Sample Output:
   3

 When you're ready to test your code, hit 'Test'.
 Your bot will be put up against a random opponent.
 Fix any bugs, and when you're happy, hit 'Deploy'.  */

function(edn_state) {
  var state = edn_to_json(edn_state);

  var trophy = state["current-trophy"];
  return trophy;
}
