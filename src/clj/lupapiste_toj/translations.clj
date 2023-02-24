(ns lupapiste-toj.translations
  (:require [com.stuartsierra.component :as component]
            [lupapiste-toj.i18n :as i18n]
            [taoensso.timbre :as timbre]))

(defrecord Translations []
  component/Lifecycle
  (start [this]
    (timbre/info "Loading translations")
    (i18n/load-translations)
    this)
  (stop [this]
    this))
