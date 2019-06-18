(ns badge.commands.core
  (:import [java.io File])
  (:require [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [taoensso.timbre :as t]
            [badge.handlers :as h]
            [badge.config :as config]))

(defn purge-files [] (h/purge-files))


(defn purge-cache [] (h/purge-cache))
