(ns auth-server.handler-test
  (:require
   [auth-server.config :refer [env]]
   [auth-server.db.core :as db]
   [auth-server.handler :refer :all]
   [auth-server.middleware.formats :as formats]
   [clojure.test :refer :all]
   [clojure.string :refer [starts-with?]]
   [luminus-migrations.core :as migrations]
   [muuntaja.core :as m]
   [mount.core :as mount]
   [ring.mock.request :refer :all]
   [ring.util.codec :refer [url-decode url-encode]]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'auth-server.config/env
                 #'auth-server.handler/app-routes
                 #'auth-server.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-ui
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "login route"
    (let [response ((app) (request :get "/login"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response))))))

(deftest test-authorize
  (testing "redirects to login"
    (let [response ((app) (request :get "/api/authorize" {:response_type "code"
                                                          :client_id "test"
                                                          :redirect_uri "http://localhost:3000/redirect?a-query=value"}))]
      (is (= 302 (:status response)))
      (is (= (str "http://localhost/login?client=test&type=code&redirect_uri=" (url-encode "http://localhost:3000/redirect?a-query=value"))
             (get-in response [:headers "Location"])))))

  (testing "authorize incorrect response type"
    (let [response ((app) (request :get "/api/authorize" {:response_type "incorrect"
                                                          :client_id "test"
                                                          :redirect_uri "http://localhost:3000"}))]
      (is (= 400 (:status response)))))

  (testing "authorize missing response type"
    (let [response ((app) (request :get "/api/authorize" {:client_id "test"
                                                          :redirect_uri "http://localhost:3000"}))]
      (is (= 400 (:status response)))))

  (testing "authorize missing client id"
    (let [response ((app) (request :get "/api/authorize" {:response_type "code"
                                                          :redirect_uri "http://localhost:3000"}))]
      (is (= 400 (:status response)))))

  (testing "authorize missing redirect uri"
    (let [response ((app) (request :get "/api/authorize" {:response_type "code"
                                                          :client_id "test"}))]
      (is (= 400 (:status response))))))

(deftest test-login
  (testing "valid crendentials log the user in"
    (let [response ((app) (request :post "/api/login" {:username "user"
                                                       :password "pass"}))]
      (is (= 302 (:status response)))
      (is (= "http://localhost:80"
             (get-in response [:headers "Location"])))))

  (testing "invalid credentials fail"
    (let [response ((app) (request :post "/api/login" {:username "user"
                                                       :password "whoops"}))]
      (is (= 401 (:status response)))))

  (testing "generates an auth code"
    (let [response ((app) (request :post "/api/login" {:username "user"
                                                       :password "pass"
                                                       :client "client"
                                                       :type "code"
                                                       :redirect_uri "http://host:8080/path"}))]
      (is (= 302 (:status response)))
      (is (starts-with? (get-in response [:headers "Location"])
                        "http://host:8080/path?code=eyJhbGciOiJIUzI1NiJ9")))))
