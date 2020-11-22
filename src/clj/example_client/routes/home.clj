(ns example-client.routes.home
  (:require
   [selmer.parser :as parser]
   [ring.util.http-response :refer [content-type ok]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defn- render
  "renders the HTML template located relative to resources/html"
  [template & [params]]
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
  (fn [_] (render template)))

(defn home-routes []
  [""
   ["/" {:get (get-page "index.html")}]])
