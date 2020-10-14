(ns auth-server.routes.urls
  (:require [clojure.java.io :as io]
            [clojure.string :as s]))

(defn- to-param-str [params]
  (->>
   (seq params)
   (map (fn [[k v]] (str (name k) "=" v)))
   (s/join "&")))

(defn add-params-to-url [url params]
  (let [parsed-url (io/as-url url)
        param-str (to-param-str params)]
    (if (empty? (.getQuery parsed-url))
      (str url "?" param-str)
      (str url "&" param-str))))

(defn base-url [request]
  (str (-> request :scheme name) "://"
       (:server-name request) ":"
       (:server-port request)))
