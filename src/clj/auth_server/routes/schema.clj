(ns auth-server.routes.schema
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]))

(s/def ::access_token string?)
(s/def ::code string?)
(s/def ::client_id string?)
(s/def ::expires_in number?)
(s/def ::grant_type #{"authorization_code" "client_credentials"})
(s/def ::password string?)
(s/def ::redirect_uri (s/and string?
                             #(try
                                (io/as-url %)
                                (catch Throwable t false))))
(s/def ::refresh_token string?)
(s/def ::response_type #{"code"})
(s/def ::scope string?)
(s/def ::state string?)
(s/def ::token_type string?)
(s/def ::type #{"code"})
(s/def ::username string?)

(s/def ::authorize-query-params (s/keys :req-un [::response_type ::client_id ::redirect_uri]
                                        :opt-un [::scope ::state]))

(s/def ::login-form-params (s/keys :req-un [::username ::password]
                                   :opt-un [::client_id ::redirect_uri ::type]))

(s/def ::token-form-params (s/keys :req-un [::grant_type]))

(s/def ::token-form-auth-code-params (s/keys :req-un [::client_id ::code ::grant_type ::redirect_uri]))

(s/def ::token-response-body (s/keys :req-un [::access_token ::expires_in]))

(s/def ::exp number?)
(s/def ::iat number?)
(s/def ::sub string?)

(s/def ::authorization-code (s/keys :req-un [::client_id ::exp ::iat ::sub]))
