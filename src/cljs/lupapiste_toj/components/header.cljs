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
    {:language-open? false
     :user-open?     false})
  (render-state [_ {:keys [language-open? user-open?]}]
    (let [user (om/observe owner (state/user))
          user-name (display-name user)]
      (html
        [:nav.nav-wrapper
         [:div.nav-top
          [:div.nav-box
           [:div.brand
            [:a.logo.lupapiste-logo {:href  (app-link (name current-lang) "authority#!/applications")
                                     :style {:background      (str "url(" (routing/path "/img/lupapiste-logo.svg") ")")
                                             :background-size "164px 35px"}}
             ""]]
           [:div#language-select {:class (if language-open? "language-open" "language-closed")}
            [:button {:on-click (fn [] (om/update-state! owner :language-open? not))}
             [:span (name current-lang)]
             [:span {:class (if language-open? "lupicon-chevron-small-up" "lupicon-chevron-small-down")}]]]
           [:div.tos-logo (t "Tiedonohjaussuunnitelma")]
           (when-not (= :unset current-lang)
             [:div.header-menu
              [:a.btn.navi.wide-icon-only {:href (app-link (name current-lang) "authority#!/applications") :title (t "navigation.dashboard")}
               [:i.lupicon-documents]
               [:span (t "navigation.dashboard")]]
              [:a.btn.navi.wide-icon-only {:href (str "/document-search?" (name current-lang)) :title (t "Dokumentit")}
               [:i.lupicon-archives]
               [:span (t "Dokumentit")]]
              [:a.btn.navi.wide-icon-only {:href (t "path.guide") :target "_blank" :title (t "help")}
               [:i.lupicon-circle-question]
               [:span (t "help")]]
              [:div#header-user-dropdown.header-dropdown {:class (when user-open? "active")}
               [:button.navi {:on-click #(om/set-state! owner :user-open? (not user-open?))}
                [:i.lupicon-user]
                [:span#user-name
                 (or user-name (t "Ei käyttäjää"))]
                [:i {:class (if user-open? "lupicon-chevron-small-up" "lupicon-chevron-small-down")}]]
               (when user-open?
                 [:ul.user-dropdown
                  [:li
                   [:a.btn.navi {:href  (app-link (name current-lang) "#!/mypage")
                                 :title (t "mypage.title")}
                    [:i.lupicon-user]
                    [:span (t "mypage.title")]]]
                  [:li
                   [:a.btn.navi {:href (app-link (name current-lang) "logout") :title (t "logout")}
                    [:i.lupicon-log-out]
                    [:span (t "logout")]]]])]

              ])]]
         (when language-open?
           [:div.nav-bottom
            [:div.nav-box
             [:div.language-menu
              [:ul
               (doall
                 (for [lang [:fi :sv :en]]
                   [:li
                    [:button.navi {:on-click (fn []
                                               (state/fetch-translations lang)
                                               (om/set-state! owner :language-open? false))}
                     (str (string/upper-case (name lang)) " - " (t lang))]]))]]]])]))))
