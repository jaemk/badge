(ns badge.handlers
  (:import [java.io File]
           [io.netty.buffer PooledSlicedByteBuf]
           [java.time Instant]
           [java.time.format DateTimeFormatter])
  (:require [taoensso.timbre :as t]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [manifold.time :as dtime]
            [byte-streams :as bs]
            [aleph.http :as http]
            [ring.util.codec :refer [form-encode]]
            [ring.util.mime-type :refer [ext-mime-type]]
            [ring.util.response :as r]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [hiccup.core :as hi]
            [badge.execution :as ex]
            [badge.utils :as u :refer [->resp ->html ->json]]
            [badge.config :as config]))


(def body "
Welcome to badge-cache!

Usage:
    - Get a crate's badge:
        /crate/&ltcrate-name&gt?&ltshields-io-params&gt
        ex. /crate/iron?label=iron&style=flat-square <img src='/crate/iron?label=iron&style=flat-square' />


        (shields.io compatible url)
        /crates/v/&ltcrate-name&gt.svg?&ltshields-io-params&gt
        ex. /crates/v/mime.svg?label=mime <img src='/crates/v/mime.svg?label=mime' />
        ex. /crates/v/mime.png?label=mime <img src='/crates/v/mime.png?label=mime' />
        ex. /crates/v/mime.jpg?label=mime <img src='/crates/v/mime.jpg?label=mime' />

        ex. /crates/v/mime.json?label=mime
<span id='json-info'><noscript> I can't load without javascript -_- </noscript></span>


    - Get a generic badge:
        /badge/&ltbadge-info-triple&gt?&ltshields-io-params&gt
        ex. /badge/custom-long--status--note-blue?style=flat-square <img src='/badge/custom-long--status--note-blue?style=flat-square' />


        (shields.io compatible url)
        /badge/&ltbadge-info-triple&gt.svg?&ltshields-io-params&gt
        ex. /badge/custom-status-x.svg?style=social <img src='/badge/custom-status-x.svg?style=social' />


    - Force a server cache reset:
        ex.
            curl -X POST https://badge-cache.kominick.com/reset/crate/mime.jpg?label=mime
            curl -X POST https://badge-cache.kominick.com/reset/crates/v/mime.jpg?label=mime
            curl -X POST https://badge-cache.kominick.com/reset/badge/custom-status-x.svg?style=social
")


(def script "
document.addEventListener('DOMContentLoaded', function() {
    var jsonInfo = document.getElementById('json-info');
    http = new XMLHttpRequest();
    var url = '/crate/mime.json?label=mime';
    http.open('GET', url, true);
    http.onreadystatechange = function() {
        if (http.readyState !== XMLHttpRequest.DONE || http.status !== 200) { return; }
        jsonInfo.textContent = http.responseText;
    }
    http.send();
});
")


(defn render [{:keys [head body script]
               :or {head nil
                    body nil
                    script nil}}]
  (hi/html
    [:html
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:head
     [:title "badge-cache"
      [:link {:rel "shortcut icon"
              :href "/favicon.ico?v=1"
              :type "image/x-icon"}]
      [:link {:rel "stylesheet"
              :href "/static/css/base.css"}]]
     head]
    [:body body]
    [:footer {:id "footer"}
     [:a {:href "https://github.com/jaemk/badge-cache"}
      [:img {:style "width: 32px;"
             :src "static/github.png"}]]]
    [:script script]]))


(defn index [_]
  (-> {:body [:pre body]
       :script script}
      (render)
      (->html)))


(defonce cache (atom {}))


(defn stream-to-file [src file]
  (d/future-with
    ex/pool
    (with-open [file-stream (io/output-stream file)]
      (io/copy src file-stream))))


(defn make-file-name [params]
  (format
    "%s/%s_%s_%s.%s"
    (config/v :badge-dir)
    (:badge-name params)
    (-> params :url-params form-encode)
    (:created-ms params)
    (:ext params)))


(defmulti make-url (fn [params] (:kind params)))

(defmethod make-url :crate [params]
  (format
    "https://img.shields.io/crates/v/%s.%s%s"
    (:badge-name params)
    (:ext params)
    (if (empty? (:url-params params))
      ""
      (str "?" (-> params :url-params form-encode)))))

(defmethod make-url :badge [params]
  (format
    "https://img.shields.io/badge/%s.%s%s"
    (:badge-name params)
    (:ext params)
    (if (empty? (:url-params params))
      ""
      (str "?" (-> params :url-params form-encode)))))


(defn now-millis [] (System/currentTimeMillis))
(def cache-ttl (* 60 60 24 1000))


(defn from-cache [url]
  (when-let [cached (get @cache url)]
    (let [[file-name created-ms] cached
          log-args {:url url
                    :file-name file-name
                    :created-ms created-ms}]
      (if (or (< created-ms (- (now-millis) cache-ttl))
              (not (.exists (io/file file-name))))
        ;; expired
        (do
          (t/info "found expired badge" log-args)
          (swap! cache #(dissoc % url))
          nil)
        ;; ok
        (do
          (t/info "found cached badge" log-args)
          file-name)))))


(defn into-cache [resp url file-name created-ms]
  (d/chain
    (stream-to-file (:body resp) (io/file file-name))
    (fn [_] (swap! cache #(assoc % url [file-name created-ms])))
    (fn [_]
      (t/info "saved cached file" {:url url :file-name file-name})
      file-name)))


(defn get-cached-badge [params]
  (let [url (make-url params)
        file-name (make-file-name params)]
    (if-let [cached (from-cache url)]
      cached
      (do
        (t/info "retrieving fresh badge" params)
        (d/chain
          (http/get url {:pool ex/cp})
          (fn [resp]
            (into-cache resp url file-name (:created-ms params))))))))


(defn badge-params [req-params]
  (t/info "building param map" {:req-params req-params})
  (let [badgename (:badge-name req-params)
        url-params (dissoc req-params :badge-name)

        [-name ext] (u/file->name-parts badgename)]
    {:badge-name -name
     :ext ext
     :url-params url-params
     :created-ms (now-millis)}))


(defn add-mime-type [resp file-path]
  (if-let [mime-type (ext-mime-type file-path)]
    (r/content-type resp mime-type)
    resp))


(defn add-expiry
  ([resp] (add-expiry resp (* 60 60)))
  ([resp expiry-s]
   (-> resp
       (r/header "cache-control" (format "max-age=%d, public" expiry-s))
       (r/header "expires"
                 (-> (Instant/now)
                     (.plusMillis (* 1000 expiry-s))
                     (.atZone config/gmt-zone)
                     (.format DateTimeFormatter/RFC_1123_DATE_TIME))))))


(defn fetch-badge-with-expiry [params]
  (->
    (d/chain
      (get-cached-badge params)
      (fn [file-path]
        (let [f (io/file file-path)]
          (if-not (.exists f)
            nil
            (->
              (->resp :body (io/file file-path))
              (add-mime-type file-path)
              (add-expiry))))))
    (d/catch
      Exception
      (fn [e]
        (let [url (make-url params)]
          (t/error "unexpected error - redirecting to source"
                   {:ex-data e
                    :url url})
          (->resp
            :status 302
            :headers {"location" (make-url params)}))))))


(defn badge [r kind]
  (let [params (-> r
                   :params
                   badge-params
                   (assoc :kind kind))]
    (fetch-badge-with-expiry params)))


(defn reset [r kind]
  (let [params (-> r :params badge-params (assoc :kind kind))
        url (make-url params)]
    (t/info "reseting badge" {:url url :params params})
    (swap! cache #(dissoc % url))))


(defn purge-files []
  (let [dir (io/file (config/v :badge-dir))
        files (filter #(not (.isDirectory %)) (.listFiles dir))
        len (count files)]
    (-> (map #(.delete %) files)
        ((fn [attempts]
          (let [successes (count (filter true? attempts))
                data {:present len
                      :deleted successes
                      :failed (- len successes)}]
            (t/info "purged files" data)
            (->json {:ok "ok"
                     :data data})))))))


(defn purge-cache []
  (reset! cache {})
  (->json {:ok "ok"}))
