(ns badge.utils
  (:require [taoensso.timbre :as t]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.util UUID]
           [java.nio ByteBuffer]
           [java.io File]
           [org.apache.commons.codec.binary Hex]))


;; ---- response builders
(defn ->resp
  "Construct a response map
  Any kwargs provided are merged into a default 200 response"
  [& kwargs]
  (let [kwargs (apply hash-map kwargs)
        headers (if-let [ct (:ct kwargs)]
                  {"content-type" ct}
                  {})
        kwargs (dissoc kwargs :ct)
        default {:status 200
                 :headers headers
                 :body ""}]
    (merge default kwargs)))


(defn ->text [s & kwargs]
  (let [kwargs (apply hash-map kwargs)
        s (if (instance? String s) s (str s))]
    (merge
      {:status 200
       :headers {"content-type" "text/plain"}
       :body s}
      kwargs)))


(defn ->html [s & kwargs]
  (let [kwargs (apply hash-map kwargs)
        s (if (instance? String s) s (str s))]
    (merge
      {:status 200
       :headers {"content-type" "text/html"}
       :body s}
      kwargs)))


(defn ->json [mapping & kwargs]
  (let [kwargs (apply hash-map kwargs)]
    (merge
      {:status 200
       :headers {"content-type" "application/json"}
       :body (json/encode mapping)}
      kwargs)))


;; ---- error builders
(defn ex-invalid-request!
  [& {:keys [e-msg resp-msg] :or {e-msg "invalid request"
                                  resp-msg "invalid request"}}]
  (throw
    (ex-info e-msg
             {:type :invalid-request
              :msg e-msg
              :resp (->resp :status 400 :body resp-msg)})))

(defn ex-unauthorized!
  [& {:keys [e-msg resp-msg] :or {e-msg "unauthorized"
                                  resp-msg "unauthorized"}}]
  (throw
    (ex-info e-msg
             {:type :invalid-request
              :msg e-msg
              :resp (->resp :status 401 :body resp-msg)})))

(defn ex-not-found!
  [& {:keys [e-msg resp-msg] :or {e-msg "item not found"
                                  resp-msg "item not found"}}]
  (throw
    (ex-info e-msg
             {:type :invalid-request
              :msg e-msg
              :resp (->resp :status 404 :body resp-msg)})))

(defn ex-does-not-exist! [record-type]
  (let [msg (format "%s does not exist" record-type)]
    (throw
      (ex-info
        msg
        {:type :does-not-exist
         :cause record-type
         :msg msg
         :resp (->resp :status 404 :body "item not found")}))))

(defn ex-error!
  [e-msg & {:keys [resp-msg cause] :or {resp-msg "something went wrong"
                                        cause nil}}]
  (throw
   (ex-info
     e-msg
     {:type :internal-error
      :cause cause
      :msg e-msg
      :resp (->resp :status 500 :body resp-msg)})))


;; ---- general
(defn uuid []
  (UUID/randomUUID))


(defn format-uuid [^UUID uuid]
  (-> uuid .toString (.replace "-" "")))


(defn parse-uuid [^String uuid-str]
  (when (some? uuid-str)
    (try
      (-> (Hex/decodeHex uuid-str)
          ((fn [^"[B" buf]
            (if (not (= 16 (alength buf)))
              (throw (Exception. "invalid uuid"))
              buf)))
          (ByteBuffer/wrap)
          ((fn [^ByteBuffer buf]
             (UUID. (.getLong buf) (.getLong buf)))))
      (catch Exception e
        (t/error {:exc-info e})
        (throw (Exception. "Invalid uuid"))))))


(defn parse-int [s]
  (Integer/parseInt s))


(defn parse-bool [s]
  (Boolean/parseBoolean s))


(defn kebab->under [s]
  (string/replace s "-" "_"))


(defn under->kebab [s]
  (string/replace s "_" "-"))


(defn pad-vec
  ([coll size] (pad-vec coll size nil))
  ([coll size pad]
   (as-> coll v
         (concat v (repeat pad))
         (take size v)
         (vec v))))


(defn file->name [f]
  (if (instance? File f) (str f) f))


(defn file->name-parts [f & [{:keys [default-ext] :or {default-ext "svg"}}]]
  (let [f-name (file->name f)
        parts (string/split f-name #"\.")

        len (count parts)]
    (if (= len 1)
      [f-name default-ext]
      (let [f-ext (last parts)
            name-parts (take (dec len) parts)
            f-name (string/join "." name-parts)]
        [f-name f-ext]))))
