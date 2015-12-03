(ns hidden-reef-3079.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.json]
            [ring.util.response :as ring-resp]))

(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))

(def mock-project-collection
  {:sleeping-cat {:name "Sleeping Cat Project"
                  :framework "Pedestal"
                  :language "Clojure"
                  :repo "https://gitlab.com/srehorn/sleepingcat"}
   :stinky-dog {:name "Stinky Dog Experiment"
                :framework "Grails"
                :language "Groovy"
                :repo "https://gitlab.com/srehorn/stinkydog"}})

(defn add-project
  [request]
  (let [incoming (:json-params request)
        connect-string (System/getenv "MONGO_CONNECTION")
        {:keys [conn db]} (mg/connect-via-uri connect-string)]
    (ring-resp/created
     "http://my-created-resource-url"
     (mc/insert-and-return db "project-catalog" incoming))))

(defn home-page
  [request]
  (ring-resp/response "Hello from Heroku!"))

(defn db-get-project [proj-name]
  (let [uri (System/getenv "MONGO_CONNECTION")
        {:keys [conn db]} (mg/connect-via-uri uri)]
    (mc/find-maps db "project-catalog" {:proj-name proj-name})))

(defn get-project
  [request]
  (bootstrap/json-response (db-get-project
                            (get-in request [:path-params :proj-name]))))

(defn get-projects
  [request]
  (let [uri (System/getenv "MONGO_CONNECTION")
        {:keys [conn db]} (mg/connect-via-uri uri)]
    (bootstrap/json-response
     (mc/find-maps db "project-catalog"))))

(defroutes routes
  ;; Defines "/" and "/about" routes with their associated :get handlers.
  ;; The interceptors defined after the verb map (e.g., {:get home-page}
  ;; apply to / and its children (/about).
  [[["/" {:get home-page}
     ^:interceptors [(body-params/body-params) bootstrap/html-body]
     ["/projects" {:get get-projects
                   :post add-project}]
     ["/projects/:proj-name" {:get get-project}]
     ["/about" {:get about-page}]]]])

;; Consumed by hidden-reef-3079.server/create-server
;; See bootstrap/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::bootstrap/interceptors []
              ::bootstrap/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::bootstrap/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::bootstrap/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::bootstrap/type :jetty
              ;;::bootstrap/host "localhost"
              ::bootstrap/port (Integer. (or (System/getenv "PORT") 5000))})

