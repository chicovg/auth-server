(ns auth-server.db.core-test
  (:require
   [auth-server.db.core :refer [*db*] :as db]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [next.jdbc :as jdbc]
   [auth-server.config :refer [env]]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'auth-server.config/env
     #'auth-server.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-users
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (is (= 1 (db/create-user!
              t-conn
              {:username   "ssmith"
               :password   "pass"
               :admin      true}
              {})))
    (is (= {:username   "ssmith"
            :password   "pass"
            :admin      true
            :is_active  true}
           (-> (db/get-user t-conn {:username "ssmith"} {})
               (dissoc :last_login))))
    (is (= 1 (db/update-user-last-login! t-conn {:username "ssmith"})))
    (is (= 1 (db/update-user-is-active! t-conn {:username "ssmith" :is_active false})))
    (is (false? (-> (db/get-user t-conn {:username "ssmith"})
                    :is_active)))
    (is (= 1 (db/delete-user! t-conn {:username "ssmith"})))))
