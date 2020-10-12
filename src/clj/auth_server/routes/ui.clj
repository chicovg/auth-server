(ns auth-server.routes.ui
  (:require
   [auth-server.db.core :refer [*db*] :as db]
   [buddy.hashers :as h]
   [selmer.parser :as parser]
   [selmer.filters :as filters]
   [ring.util.http-response :refer [content-type ok]]
   [ring.util.response :refer [redirect]]
   [ring.util.anti-forgery :refer [anti-forgery-field]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(parser/set-resource-path!  (clojure.java.io/resource "html"))
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

(defn login-user [request]
  (let [{:keys [username password next]} (:params request)
        user (db/get-user *db* {:username username})]
    (if (h/check password (:password user))
      (-> (redirect (if (empty? next) "/" next))
          (assoc-in [:session] (-> (:session request)
                                   (assoc :identity (:username user)))))
      (render request "login.html" {:error "Invalid credentials provided"}))))

(defn ui-routes []
  [""
   ["/" {:get (get-page "home.html")}]
   ["/login" {:get (get-page "login.html")
              :post login-user}]])
