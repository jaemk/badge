(ns badge.types
  (:require [clojure.spec.alpha :as s])
  (:import (java.util Date UUID)))


(def kw-namespaces #{})

(defn date? [d]
  (instance? Date d))

(defn nullable [check]
  (fn [v]
    (or (nil? v)
        (check v))))

(s/def ::id int?)
(s/def ::size (nullable int?))
(s/def ::user_id int?)

(s/def ::name string?)
(s/def ::hash string?)

(s/def ::token uuid?)
(s/def ::created date?)

(s/def ::user
  (s/keys :req-un [::id ::name ::created]))

(s/def ::auth
  (s/keys :req-un [::id ::user_id ::token ::created]))

(s/def ::where map?)
