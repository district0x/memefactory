(ns memefactory.ui.get-dank.events
  (:require
    [ajax.core :as ajax]
    [day8.re-frame.http-fx]
    [district.ui.logging.events :as logging]
    [district.ui.notification.events :as notification-events]
    [memefactory.ui.config :refer [config-map]]
    [re-frame.core :as re-frame]
    [taoensso.timbre :as log]))


(re-frame/reg-event-fx
 ::show-spinner
 (fn [{:keys [db]} [_ _]]
   {:db (assoc db
          ::spinner true)}))


(re-frame/reg-event-fx
 ::hide-spinner
 (fn [{:keys [db]} [_ _]]
   {:db (assoc db
          ::spinner false)}))


(re-frame/reg-event-fx
 ::get-allocated-dank-twitter
 (fn [{:keys [db]} [_ tweet-url]]
   {:http-xhrio {:method          :post
                 :uri             (get-in config-map [:dank-faucet-twitter :url])
                 :params          {:tweet-url tweet-url}
                 :timeout         30000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :format          (ajax/json-request-format)
                 :on-success      [::dank-faucet-twitter-response]
                 :on-failure      [::faucet-error "failed to process request"]}
    :dispatch [::show-spinner]
    }))


(re-frame/reg-event-fx
  ::dank-faucet-twitter-response
  (fn [{:keys [db]} [_ http-resp]]
    (let [status (get-in http-resp [:status])]
      (if (not= status "success")
        (let [message (get-in http-resp [:message])]
          {:dispatch [::faucet-error message {}]})
        {:dispatch [::faucet-succeed]}))))


(re-frame/reg-event-fx
  ::faucet-error
  (fn [{:keys [db]} [_ message & opts]]
    {:dispatch-n [[::notification-events/show {:show-duration 6000
                                               :message message}]
                  [::logging/error "Error verifying tweet URL" message opts]
                  [::hide-spinner]]}))


(re-frame/reg-event-fx
  ::faucet-succeed
  (fn [{:keys [db]} [_]]
    {:db (assoc db ::succeeded true)
     :dispatch-n [[::logging/info "DANK successfully requested"]
                  [::hide-spinner]
                  [::notification-events/show {:show-duration 8000
                                               :message "DANK allotment requested. You should receive your DANK shortly"}]]}))

