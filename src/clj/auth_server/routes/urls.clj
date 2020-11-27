(ns auth-server.routes.urls
  (:require [cemerick.url :refer [url]]))

(defn add-query-params [raw-url params]
  (let [parsed-url (url raw-url)
        query (merge (:query parsed-url)
                     params)]
    (-> parsed-url
        (assoc :query query)
        str)))

(defn set-hash [raw-url hash]
  (let [parsed-url (url raw-url)
        parsed-hash (:anchor parsed-url)
        full-hash (if parsed-hash
                    (str parsed-hash "/" hash)
                    (str "/" hash))]
    (-> parsed-url
        (assoc :anchor full-hash)
        str)))

(comment
  (set-hash "http://localhost:4000#/redirect-with-token" "token=foo")
   ;; http://localhost:4000#/redirect-with-token/token=foo

  (set-hash "http://localhost:3000" "token=foo")
   ;; http://localhost:3000#/token=foo

  ;; let see how the router handles this...
  (require '[reitit.core :as r])

  (def router
    (r/router
     [["/" :home]
      ["/redirect-with-code" :redirect-with-code]
      ["/redirect-with-token/token=:token" :redirect-with-token]
      ["/user" :user]]))

  (r/match-by-path router "/redirect-with-token/token=foo")

  ;; #reitit.core.Match{:template "/redirect-with-token/token=:token",
  ;;                  :data {:name :redirect-with-token},
  ;;                  :result nil,
  ;;                  :path-params {:token "foo"},
  ;;                  :path "/redirect-with-token/token=foo"}

  )

(defn base-url [request]
  (str (-> request :scheme name) "://"
       (:server-name request) ":"
       (:server-port request)))
