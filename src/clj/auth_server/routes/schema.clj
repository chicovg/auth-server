(ns auth-server.routes.schema
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]))

(s/def ::access_token string?)
(s/def ::address string?)
(s/def ::city string?)
(s/def ::client_id string?)
(s/def ::code string?)
(s/def ::email string?) ;; TODO email type
(s/def ::expires_in number?)
(s/def ::first_name string?)
(s/def ::grant_type #{"authorization_code" "client_credentials" "password"})
(s/def ::last_name string?)
(s/def ::password string?)
(s/def ::redirect_uri (s/and string?
                             #(try
                                (io/as-url %)
                                (catch Throwable t false))))
(s/def ::refresh_token string?)
(s/def ::response_type #{"code" "token"})
(s/def ::scope string?)
(s/def ::state string?)
(s/def ::token_type string?)
(s/def ::type #{"code" "token"})
(s/def ::username string?)
(s/def ::zipcode string?) ;; TODO zipcode type

(s/def ::authorize-query-params (s/keys :req-un [::response_type ::client_id ::redirect_uri]
                                        :opt-un [::scope ::state]))

(s/def ::login-form-params (s/keys :req-un [::username ::password]
                                   :opt-un [::client_id ::redirect_uri ::type]))

(s/def ::token-form-params (s/keys :req-un [::grant_type]))

(s/def ::token-from-auth-code-params (s/keys :req-un [::client_id ::code ::grant_type ::redirect_uri]))

(s/def ::token-from-password-params (s/keys :req-un [::grant_type ::username ::password]))

(s/def ::token-response-body (s/keys :req-un [::access_token ::expires_in]))

(s/def ::exp number?)
(s/def ::iat number?)
(s/def ::sub string?)

(s/def ::authorization-code (s/keys :req-un [::client_id ::exp ::iat ::sub]))

(s/def ::user (s/keys :req-un [::username]
                      :opt-un [::first_name ::last_name ::email ::address ::city ::state ::zipcode]))
