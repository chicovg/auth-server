(ns auth-server.routes.services
  (:require
   [auth-server.db.core :refer [*db*] :as db]
   [auth-server.middleware.formats :as formats]
   [auth-server.middleware.exception :as exception]
   [auth-server.routes.ui :refer [get-login-error-page]]
   [auth-server.routes.urls :refer [add-params-to-url base-url]]
   [buddy.hashers :as h]
   [buddy.sign.jwt :as jwt]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [clojure.string :as st]
   [reitit.core :as r]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.util.codec :refer [url-decode url-encode]]
   [ring.util.response :refer [redirect]]
   [ring.util.http-response :refer [bad-request ok]])
  (:import (java.util Date)))

(s/def ::response_type #(= % "code"))
(s/def ::client_id string?)
(s/def ::redirect_uri (s/and string?
                             #(try
                                (io/as-url %)
                                (catch Throwable t false))))
(s/def ::scope string?)
(s/def ::state string?)
(s/def ::username string?)
(s/def ::password string?)
(s/def ::type #{"code"})

(s/def ::authorize-query-params (s/keys :req-un [::response_type ::client_id ::redirect_uri]
                                        :opt-un [::scope ::state]))

(s/def ::login-form-params (s/keys :req-un [::username ::password]
                                   :opt-un [::redirect_uri ::type]))

(defn param [key value]
  (str key "=" value))

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"
             :config {:validator-url nil}})}]]

   ["/ping"
    {:get (constantly (ok {:message "pong"}))}]

   ;; authorization
   ["/authorize" {:get {:summary "An endpoint used to request authorization of a user on behalf of a client"
                        :parameters {:query ::authorize-query-params}
                        :responses {302 nil
                                    400 {:body {:error string?}}}
                        :handler (fn [{{{:keys [client_id redirect_uri]} :query} :parameters}]
                                   (if (nil? (db/get-client {:id client_id}))
                                     {:status 401 :body "Invalid client id"}
                                     (redirect (str "/login"
                                                    "?client_id=" client_id
                                                    "&type=code"
                                                    "&redirect_uri=" (url-encode redirect_uri)))))}}]

   ["/login" {:post {:summary "An endpoint which validates user credentials and redirects to approprate page"
                     :parameters {}
                     :responses {302 nil
                                 400 {:body {:error string?}}}
                     :handler (fn [{{:keys [username password client_id redirect_uri type]} :params :as request}]
                                (let [user (db/get-user {:username username})
                                      next_uri (or redirect_uri (base-url request))
                                      iat (.getTime (new Date))
                                      exp (+ iat (* 10 60 1000))]
                                  (cond
                                    (not (h/check password (:password user)))
                                    (get-login-error-page request 401 "Invalid credentials")

                                    (and (not (nil? type)) (nil? (db/get-client {:id client_id})))
                                    (get-login-error-page request 401 "Invalid client id")

                                    (= type "code")
                                    (-> (add-params-to-url next_uri {:code (jwt/sign {:sub username
                                                                                      :client_id client_id
                                                                                      :iat iat
                                                                                      :exp exp}
                                                                                     "secret")})
                                        (redirect))

                                    :else
                                    (-> (redirect next_uri)
                                        (assoc-in [:session :identity] username)))))}}]

   ;; ["/math"
   ;;  {:swagger {:tags ["math"]}}

   ;;  ["/plus"
   ;;   {:get {:summary "plus with spec query parameters"
   ;;          :parameters {:query {:x int?, :y int?}}
   ;;          :responses {200 {:body {:total pos-int?}}}
   ;;          :handler (fn [{{{:keys [x y]} :query} :parameters}]
   ;;                     {:status 200
   ;;                      :body {:total (+ x y)}})}
   ;;    :post {:summary "plus with spec body parameters"
   ;;           :parameters {:body {:x int?, :y int?}}
   ;;           :responses {200 {:body {:total pos-int?}}}
   ;;           :handler (fn [{{{:keys [x y]} :body} :parameters}]
   ;;                      {:status 200
   ;;                       :body {:total (+ x y)}})}}]]

   ;; ["/files"
   ;;  {:swagger {:tags ["files"]}}

   ;;  ["/upload"
   ;;   {:post {:summary "upload a file"
   ;;           :parameters {:multipart {:file multipart/temp-file-part}}
   ;;           :responses {200 {:body {:name string?, :size int?}}}
   ;;           :handler (fn [{{{:keys [file]} :multipart} :parameters}]
   ;;                      {:status 200
   ;;                       :body {:name (:filename file)
   ;;                              :size (:size file)}})}}]

   ;;  ["/download"
   ;;   {:get {:summary "downloads a file"
   ;;          :swagger {:produces ["image/png"]}
   ;;          :handler (fn [_]
   ;;                     {:status 200
   ;;                      :headers {"Content-Type" "image/png"}
   ;;                      :body (-> "public/img/warning_clojure.png"
   ;;                                (io/resource)
   ;;                                (io/input-stream))})}}]]
   ])
