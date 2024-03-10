(ns cloregram.logging
  (:require [cloregram.utils :as utl]
            [cheshire.core :refer [parse-string]]
            [dialog.logger]))

(defn- extract-json [s]
  (let [possible-json (re-find #" \{.*\}$| \[.*\]$" s)]
    (try
      (when possible-json
        [(parse-string possible-json true) (count possible-json)])
      (catch Exception e
        nil))))

(defn- manipulate-message
  [event]
  (if-let [[data length] (extract-json (:message event))]
    (-> event
        (assoc :data data)
        (update :message #(drop-last length %)))
    event))

(defn transform-json
  [_ event]
  (-> event
    (dissoc :line)
    (merge (utl/get-project-info))
    (manipulate-message)))

;; (defn -log-macro#
;;   "Pass a message into the logging system. Used by the logging macro
;;   expansion - consumers should call `log-event` directly."
;;   [event f x & more]
;;   (log-event
;;    (cond
;;      (instance? clojure.lang.ExceptionInfo x)
;;      (assoc event
;;             :message (apply f (.getMessage x) more)
;;             :error x
;;             :data (cond
;;                     (map? (:data event))
;;                     (assoc (.getData x) :more (:data event))

;;                     (seq? (:data event))
;;                     (conj (:data event) (.getData x))

;;                     :else
;;                     (conj [:data event] (.getData x)))
     
;;      (instance? Throwable x)
;;      (assoc event
;;             :message (apply f (.getMessage x) more)
;;             :error x)

;;      :else
;;      (assoc event
;;             :message (apply f x more)))))

;; (intern 'dialog.logger '-log-macro -log-macro#)
  
;; (defmacro logp#
;;   "Log a message using print style args. Can optionally take a throwable as its
;;   second arg."
;;   {:arglists '([level message & more] [level throwable message & more])}
;;   [level x & more]
;;   (throw (new Exception "Yeah!"))
;;   (let [logger (str (ns-name *ns*))
;;         line (:line (meta &form))]
;;     (cond
;;       ;; Coverage-friendly form. Args are always evaluated and passed to the
;;       ;; helper function.
;;       (coverage-mode?)
;;       `(-log-macro
;;          {:logger ~logger
;;           :level ~level
;;           :line ~line
;;           :data (if (>= 1 (count ~more)) (first ~more) ~more)}
;;          print-str
;;          ~x)

;;       ;; First argument can't be an error, use simpler form.
;;       (or (string? x) (nil? more))
;;       `(let [logger# ~logger
;;              level# ~level]
;;          (when (enabled? logger# level#)
;;            (log-event
;;              {:logger logger#
;;               :level level#
;;               :line ~line
;;               :message (str ~x)
;;               :data (if (>= 1 (count ~more)) (first ~more) ~more))})))

;;       ;; General form.
;;       :else
;;       `(let [logger# ~logger
;;              level# ~level]
;;          (when (enabled? logger# level#)
;;            (-log-macro
;;              {:logger logger#
;;               :level level#
;;               :line ~line
;;               :data (if (>= 1 (count ~more)) (first ~more) ~more)}
;;              print-str
;;              ~x))))))

;; (intern 'dialog.logger 'logp logp#)
