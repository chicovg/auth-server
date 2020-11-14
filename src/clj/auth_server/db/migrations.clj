(ns auth-server.db.migrations
  (:require [auth-server.config :refer [env]]
            [auth-server.db.core :as db]))

(defn fixture-data-up
  "Inserts fixture data when the environment is :dev"
  [config]
  (when (or (env :dev) (env :test))
    (db/create-user-with-hashed-pw! "admin" "pass" true)
    (let [user_id (-> (db/create-user-with-hashed-pw! "user" "pass" false)
                      (first)
                      (:id))]
      (db/create-user-details! {:user_id user_id
                                :first_name "Archie"
                                :last_name "Peakman"
                                :email "apeakmanb@rambler.com"
                                :address "76 Sage Circle"
                                :city "Hialeah"
                                :state "Florida"
                                :zipcode "33018"}))
    (db/create-client-with-hashed-secret! "dev-client" "secret" "Dev Client")))

(defn fixture-data-down
  "Deletes fixture data when the environment is :dev"
  [config]
  (when (or (env :dev) (env :test))
    (db/delete-user! {:username "admin"})
    (db/delete-user! {:username "user"})
    (db/delete-client! {:id "dev-client"})))

;; insert into MOCK_DATA (id, first_name, last_name, email, address, city, state, zipcode)
;; values (12, 'Archie', 'Peakman', 'apeakmanb@rambler.ru', '76 Sage Circle', 'Hialeah', 'Florida', '33018');
