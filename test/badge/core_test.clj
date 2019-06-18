(ns badge.core-test
  (:use midje.sweet)
  (:require [badge.utils :as u]))


(facts
  (fact
    "we can add"
    (+ 1 2) => 3
    (+ 2 3) => 5))
