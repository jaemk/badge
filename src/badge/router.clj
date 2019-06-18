(ns badge.router
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [taoensso.timbre :as t]
            [badge.utils :as u]
            [badge.handlers :as h]
            [clojure.java.io :as io]
            [badge.config :as config]))


(defn require-auth-token [r handler]
  (let [token (-> r :headers (get "x-badge-auth"))]
    (if-not (= token (config/v :auth-token))
      (u/ex-unauthorized! :e-msg (format "invalid auth token: %s" token))
      (handler))))

(defn load-routes []
  (routes
    (ANY "/" [] h/index)
    (ANY "/status" [] (u/->json {:status :ok
                                 :version (config/v :app-version)}))

    (GET "/crates/v/:badge-name" r (h/badge r :crate))
    (GET "/crate/:badge-name"    r (h/badge r :crate))
    (GET "/badge/:badge-name"    r (h/badge r :badge))

    (POST "/reset/crates/v/:badge-name" r (h/reset r :crate))
    (POST "/reset/crate/:badge-name"    r (h/reset r :crate))
    (POST "/reset/badge/:badge-name"    r (h/reset r :crate))

    (POST "/purge/files" r (require-auth-token r h/purge-files))
    (POST "/purge/cache" r (require-auth-token r h/purge-cache))

    (GET "/favicon.ico" []
         (u/->resp :body (io/file "static/favicon.ico")))
    (route/files "/static" {:root "static"})
    (route/not-found (u/->resp
                       :body "nothing to see here"
                       :status 404))))
