(ns auth-server.middleware
  (:require
   [auth-server.config :refer [env]]
   [auth-server.db.core :as db]
   [auth-server.env :refer [defaults]]
   [auth-server.routes.tokens :refer [expired?]]
   [buddy.hashers :as h]
   [ring.middleware.flash :refer [wrap-flash]]
   [ring.adapter.undertow.middleware.session :refer [wrap-session]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [buddy.auth.accessrules :refer [restrict]]
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends :as backends]))

(defn on-error [request response]
  {:status 403
   :headers {}
   :body (str "Access to " (:uri request) " is not authorized")})

(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error on-error}))

(defn client-credentials-auth-fn [request {:keys [username password]}]
  (prn username password)
  (when-let [client (db/get-client {:id username})]
    (when (h/check password (:secret client))
      username)))

(defn jwt-auth-fn [{:keys [client_id sub] :as claims}]
  (prn claims)
  (cond
    (expired? claims) false
    (and client_id sub) (let [client (db/get-client {:id client_id})
                              user (db/get-user {:username sub})]
                          (when (and client user)
                            user))
    client_id (db/get-client {:id client_id})
    :else false))

(defn wrap-auth [handler]
  (let [session-backend (backends/session)
        cc-backend (backends/basic {:realm "AuthServerApi"
                                    :authfn client-credentials-auth-fn})
        jws-backend (backends/jws {:secret (env :secret)
                                   :authfn jwt-auth-fn})]
    (-> handler
        (wrap-authentication session-backend)
        (wrap-authentication cc-backend)
        (wrap-authentication jws-backend)
        (wrap-authorization session-backend))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only true}})
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)
           (dissoc :session)))))
