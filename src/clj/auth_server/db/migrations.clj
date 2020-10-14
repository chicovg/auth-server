(ns auth-server.db.migrations
  (:require [auth-server.config :refer [env]]
            [auth-server.db.core :as db]))

(defn fixture-data-up
  "Inserts fixture data when the environment is :dev"
  [config]
  (when (or (env :dev) (env :test))
    (db/create-user-with-hashed-pw! "admin" "pass" true)
    (db/create-user-with-hashed-pw! "user" "pass" false)
    (db/create-client-with-hashed-secret! "dev-client" "secret" "Dev Client")))

(defn fixture-data-down
  "Deletes fixture data when the environment is :dev"
  [config]
  (when (or (env :dev) (env :test))
    (db/delete-user! {:username "admin"})
    (db/delete-user! {:username "user"})
    (db/delete-client! {:id "dev-client"})))
