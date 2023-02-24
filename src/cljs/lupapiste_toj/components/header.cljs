(ns lupapiste-toj.components.header
  (:require [lupapiste-toj.i18n :refer [t]]
            [lupapiste-toj.routing :as routing]
            [lupapiste-toj.state :as state]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]
            [clojure.string :as string]))

(defn display-name [user]
  (when (:firstName user)
    (str (:firstName user) " " (:lastName user))))

(defn app-link [lang path]
  (str "/app/" lang "/" path))

(defcomponent Header [current-lang owner]
  (init-state [_]
    {:language-open? false})
  (render-state [_ {:keys [language-open?]}]
    (let [user (om/observe owner (state/user))
          user-name (display-name user)]
      (html
        [:nav.nav-wrapper
         [:div.nav-top
          [:div.nav-box
           [:div.brand
            [:a.logo {:href (app-link (name current-lang) "authority#!/applications")}
             [:img {:src (routing/path "/img/lupapiste-logo.png")
                    :alt (t "Lupapiste")}]]]
           [:div#language-select {:class (if language-open? "language-open" "language-closed")}
            [:a {:href "javascript:;" :on-click (fn [] (om/update-state! owner :language-open? not))}
             [:span (name current-lang)]
             [:span {:class (if language-open? "lupicon-chevron-small-up" "lupicon-chevron-small-down")}]]]
           [:div.tos-logo (t "Tiedonohjaussuunnitelma")]
           (when-not (= :unset current-lang)
             [:div.header-menu
              [:div.header-box
               [:a {:href (app-link (name current-lang) "authority#!/applications") :title (t "navigation.dashboard")}
                [:span.header-icon.lupicon-documents]
                [:span.narrow-hide (t "navigation.dashboard")]]]
              [:div.header-box
               [:a {:href (str "/document-search?" (name current-lang)) :title (t "Dokumentit")}
                [:span.header-icon.lupicon-archives]
                [:span.narrow-hide (t "Dokumentit")]]]
              [:div.header-box
               [:a {:href (t "path.guide") :target "_blank" :title (t "help")}
                [:span.header-icon.lupicon-circle-question]
                [:span.narrow-hide (t "help")]]]
              [:div.header-box
               [:a {:href (app-link (name current-lang) "#!/mypage")
                    :title (t "mypage.title")}
                [:span.header-icon.lupicon-user]
                [:span.narrow-hide (or user-name (t "Ei käyttäjää"))]]]
              [:div.header-box
               [:a {:href (app-link (name current-lang) "logout") :title (t "logout")}
                [:span.header-icon.lupicon-log-out]
                [:span.narrow-hide (t "logout")]]]])]]
         (when language-open?
           [:div.nav-bottom
            [:div.nav-box
             [:div.language-menu
              [:ul
               (doall
                 (for [lang [:fi :sv :en]]
                   [:li
                    [:a {:href "javascript:;" :on-click (fn []
                                                          (state/fetch-translations lang)
                                                          (om/set-state! owner :language-open? false)) }
                     (str (string/upper-case (name lang)) " - " (t lang))]]))]]]])]))))
