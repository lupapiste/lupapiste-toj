(ns lupapiste-toj.components.error-message
  (:require [lupapiste-toj.i18n :refer [t]]
            [lupapiste-toj.state :as state]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]))

(defcomponent ErrorMessage [_ owner]
  (render [_]
    (let [{:keys [status text]} (om/observe owner (state/error))]
      (html
        (when status
          [:div.error-dialog
           [:div
            [:h2 text]
            [:div.buttons
             (condp = status
               400 [:a.btn.btn-primary {:href "/tiedonohjaus"} (t "OK")]
               401 [:a.btn.btn-primary {:href "/app/fi/welcome#!/login"} (t "Kirjaudu")]
               403 [:a.btn.btn-primary {:href "/"} (t "Etusivulle")]
               500 [:a.btn.btn-primary {:href "/"} (t "Etusivulle")])]]])))))
