(ns hidden-reef-3079.dbhelpers
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.json]))

(defn db-get-project [proj-name]
  (let [uri (System/getenv "MONGO_CONNECTION")
        {:keys [conn db]} (mg/connect-via-uri uri)]
    (mc/find-maps db "project-catalog" {:proj-name proj-name})))

