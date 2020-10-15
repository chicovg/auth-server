(ns auth-server.routes.services
  (:require
   [auth-server.db.core :as db]
   [auth-server.middleware :refer [wrap-restricted]]
   [auth-server.middleware.formats :as formats]
   [auth-server.middleware.exception :as exception]
   [auth-server.routes.schema :as schema]
   [auth-server.routes.tokens :refer [sign-token unsign-token expired?]]
   [auth-server.routes.ui :refer [get-login-error-page]]
   [auth-server.routes.urls :refer [add-params-to-url base-url]]
   [buddy.hashers :as h]
   [clojure.spec.alpha :as s]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.util.codec :refer [url-decode url-encode]]
   [ring.util.response :refer [redirect]]
   [ring.util.http-response :refer [bad-request ok not-implemented unauthorized]])
  (:import (java.util Date)))

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
                        :parameters {:query ::schema/authorize-query-params}
                        :responses {302 {:description "Redirects to a login page"}
                                    400 {:description "Input validation failed"}
                                    401 {:description "An unauthorized client id was provided"}}
                        :handler (fn [{{{:keys [client_id redirect_uri]} :query} :parameters}]
                                   (if (nil? (db/get-client {:id client_id}))
                                     {:status 401 :body "Invalid client id"}
                                     (redirect (str "/login"
                                                    "?client_id=" client_id
                                                    "&type=code"
                                                    "&redirect_uri=" (url-encode redirect_uri)))))}}]

   ["/login" {:post {:summary "An endpoint which validates user credentials and redirects to approprate page"
                     :parameters {:form ::schema/login-form-params}
                     :responses {302 {:description "Redirects to the provided redirect uri or the server's base uri"}
                                 400 {:description "Input validation failed"}
                                 401 {:description "Authorization failed with the given credentials and client id"}}
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
                                    (-> (add-params-to-url next_uri {:code (sign-token {:sub username
                                                                                        :client_id client_id}
                                                                                       10)})
                                        (redirect))

                                    :else
                                    (-> (redirect next_uri)
                                        (assoc-in [:session :identity] username)))))}}]

   ["/token" {:post {:summary "An endpoint which validates an authorization grant and returns a token"
                     :swagger {:consumes ["application/x-www-form-urlencoded"]
                               :produces ["application/json"]}
                     :middleware [wrap-restricted]
                     :parameters {:form ::schema/token-form-params}
                     :responses {200 {:body ::schema/token-response-body}}
                     :handler (fn [{:keys [headers identity params]}]
                                (case (:grant_type params)
                                  "authorization_code"
                                  (let [code (:code params)
                                        data (try
                                               (unsign-token code)
                                               (catch Throwable t {}))]
                                    (cond
                                      (not (s/valid? ::schema/token-from-auth-code-params params))
                                      (bad-request {:error "Invalid request params"})

                                      (not (s/valid? ::schema/authorization-code data))
                                      (bad-request {:error "Invalid authorization code"})

                                      (not (= identity (:client_id data)))
                                      (unauthorized {:error "Unauthorized client"})

                                      (nil? (db/get-user {:username (:sub data)}))
                                      (unauthorized {:error "Unauthorized user"})

                                      (expired? data)
                                      (unauthorized {:error "Token expired"})

                                      :else
                                      (ok {:access_token (sign-token (select-keys data [:sub :client_id]) 30)
                                           :expires_in (* 30 60 1000)})))

                                  "client_credentials"
                                  (ok {:access_token (sign-token {:client_id identity} 30)
                                       :expires_in (* 30 60 100)})


                                  (not-implemented {:error (str "grant_type valid but not yet implemented")})))}}]

   ])
