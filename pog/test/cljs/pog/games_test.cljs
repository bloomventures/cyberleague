(ns pog.games-test
  (:require [clojure.test :refer :all]
            [pog.games :as game]))

(deftest goofspiel-engine
  (testing "Can create an engine"
    (is (satisfies? game/IGameEngine (game/make-engine {:game/name "goofspiel"})))
    ))
