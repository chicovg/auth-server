(ns auth-server.handler-test
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :refer :all]
   [auth-server.handler :refer :all]
   [auth-server.middleware.formats :as formats]
   [muuntaja.core :as m]
   [mount.core :as mount]
   [ring.util.codec :refer [url-decode url-encode]]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'auth-server.config/env
                 #'auth-server.handler/app-routes)
    (f)))

(deftest test-app
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "login route"
    (let [response ((app) (request :get "/login"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "services"

    (testing "authorize"
      (let [response ((app) (request :get "/api/authorize" {:response_type "code"
                                                            :client_id "test"
                                                            :redirect_uri "http://localhost:3000/redirect?a-query=value"}))]
        (is (= 302 (:status response)))
        (is (= (str "http://localhost/login?next=" (url-encode "http://localhost:3000/redirect?a-query=value&code=test"))
               (get-in response [:headers "Location"])))))

    (testing "authorize redirect without query params"
      (let [response ((app) (request :get "/api/authorize" {:response_type "code"
                                                            :client_id "test"
                                                            :redirect_uri "http://localhost:3000"}))]
        (is (= 302 (:status response)))
        (is (= (str "http://localhost/login?next=" (url-encode "http://localhost:3000?code=test"))
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
        (is (= 400 (:status response)))))
    ))
