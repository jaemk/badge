(ns user
  (:require [badge.core :as app]
            [badge.config :as config]
            [badge.utils :as u]
            [badge.types :as types]
            [badge.commands.core :as cmd])
  (:use [midje.repl]))


(defn -main []
  (app/-main))
