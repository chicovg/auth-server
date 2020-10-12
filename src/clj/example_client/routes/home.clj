(ns example-client.routes.home
  (:require
   [auth-server.db.core :refer [*db*] :as db]
   [buddy.hashers :as h]
   [selmer.parser :as parser]
   [selmer.filters :as filters]
   [ring.util.http-response :refer [content-type ok]]
   [ring.util.response :refer [redirect]]
   [ring.util.anti-forgery :refer [anti-forgery-field]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defn- render
  "renders the HTML template located relative to resources/html"
  [request template & [params]]
  (let [client-template (str "client/" template)]
    (content-type
     (ok
      (parser/render-file
       client-template
       (assoc params
              :page client-template
              :csrf-token *anti-forgery-token*)))
     "text/html; charset=utf-8")))

(defn get-page [template]
  (fn [request] (render request template)))

(defn home-routes []
  [""
   ["/" {:get (get-page "home.html")}]])
