(ns auth-server.routes.ui
  (:require
   [auth-server.routes.urls :refer [add-params-to-url]]
   [auth-server.db.core :refer [*db*] :as db]
   [buddy.hashers :as h]
   [clojure.java.io :as io]
   [selmer.parser :as parser]
   [selmer.filters :as filters]
   [ring.util.http-response :refer [bad-request content-type ok]]
   [ring.util.response :refer [redirect]]
   [ring.util.anti-forgery :refer [anti-forgery-field]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(parser/set-resource-path!  (io/resource "html"))
(parser/add-tag! :csrf-field (fn [_ _] (anti-forgery-field)))

(defn- render
  "renders the HTML template located relative to resources/html"
  [request template & [params]]
  (content-type
   (ok
    (parser/render-file
     template
     (assoc params
            :page template
            :csrf-token *anti-forgery-token*)))
   "text/html; charset=utf-8"))

;; TODO I might not need this.
(defn- error-page
  "error-details should be a map containing the following keys:
   :status - error status
   :title - error title (optional)
   :message - detailed error message (optional)

   returns a response map with the error page as the body
   and the status specified by the status key"
  [error-details]
  {:status  (:status error-details)
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file "error.html" error-details)})

(defn get-page
  [template]
  (fn [request] (render request template (merge (:session request)
                                                (:query-params request)))))

(defn get-login-page
  [request]
  (let [client-data (db/get-client *db* {:id (get-in request [:params :client])})
        params (-> (:params request)
                   (assoc :client_description (:description client-data)))]
    (render request "login.html" params)))

(defn get-login-error-page
  [request status error]
  (-> request
      (assoc-in [:params :error] error)
      (get-login-page)
      (assoc :status status)))

(defn ui-routes []
  [""
   ["/" {:get (get-page "home.html")}]
   ["/login" {:get get-login-page}]])
