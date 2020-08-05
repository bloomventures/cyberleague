(fn [state] (rand-nth (vec (get-in state [:player-cards :me]))))
