(ns user)


(defn initenv []
  (require '[badge.core :as app]
           '[badge.utils :as u]
           '[badge.config :as config]
           '[badge.types :as types]
           '[badge.commands.core :as cmd]))
