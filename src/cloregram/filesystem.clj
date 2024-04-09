(ns cloregram.filesystem
  (:require [cloregram.utils :refer [get-project-info]]))

(def temp-root

  "Returns common filesystem `java.nio.file.Path` object of unique root temp folder for this run: `/tmp/<project_group>/<project_name>/<project_version>/<millis_from_epoch>`. Redefine it if you need another folder or filesystem."
  
  (let [arr (into-array (concat (map val (get-project-info)) [(str (.getTime (java.util.Date.)))]))
        path (java.nio.file.Paths/get "/tmp" arr)]
    (-> path .toFile .mkdirs)
    path))

(defn ^java.nio.file.Path temp-path

  "Returns `java.nio.file.Path` object represents `java.nio.file.Path` argument `path` resolved relatively to current temporal directory root."

  {:added "0.9.1"
   :see-also "temp-root"}
  
  [path]
  (.resolve temp-root path))
