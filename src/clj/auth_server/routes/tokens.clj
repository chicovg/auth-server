(ns auth-server.routes.tokens
  (:require
   [auth-server.config :refer [env]]
   [buddy.sign.jwt :as jwt]
   [java-time :as t]))

(defn sign-token
  ([claims] (sign-token claims (env :auth-token-time-to-live)))
  ([claims duration] (let [iat (-> (t/instant)
                                   (t/to-millis-from-epoch))
                           exp (-> (t/instant)
                                   (t/plus (t/minutes duration))
                                   (t/to-millis-from-epoch))]
                       (-> (merge claims {:iat iat :exp exp})
                           (jwt/sign (env :secret))))))

(defn unsign-token [token]
  (jwt/unsign token (env :secret)))

(defn expired? [{:keys [exp]}]
  (t/after? (t/instant) (t/instant exp)))
