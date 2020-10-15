(ns auth-server.routes.tokens
  (:require
   [auth-server.config :refer [env]]
   [buddy.sign.jwt :as jwt]
   [java-time :as t]))

(defn sign-token [claims duration]
  (let [iat (t/instant)
        exp (t/plus iat (t/minutes duration))]
    (-> (merge claims {:iat iat :exp exp})
        (jwt/sign (env :secret)))))

(defn unsign-token [token]
  (jwt/unsign token (env :secret)))

(defn expired? [{:keys [exp]}]
  (t/after? (t/instant) (t/instant exp)))
