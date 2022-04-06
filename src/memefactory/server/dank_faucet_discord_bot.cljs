(ns memefactory.server.dank-faucet-discord-bot
  (:require [cljs-node-io.core :as io :refer [slurp spit]]
            [cljs-node-io.file :refer [File]]
            [cljs.nodejs :as nodejs]
            [cljs.reader :as reader]
            [clojure.string :as string]
            [district.server.config :as config]
            [memefactory.server.utils :refer [now]]
            [memefactory.shared.utils :refer [tweet-url-regex]]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

;; Discord bot to call DANK Twitter Faucet from a discord channel.
;; Opens a connection to Discord to listen incoming messages in the Discord servers this bot is deployed.
;; It reacts to messages mentioning the bot user which include a twitter URL and invoke the Twitter Faucet if URL is correct.
;; For example: @Dank Faucet https://twitter.com/username/status/12345678901234567890
;; Also listen for commands starting with !dank-faucet to allow basic management.
;; This bot enables some limits to avoid abuse. Admins can set a request limit for specific time windows,
;; such as does not allow more than 2 request per minute, or 10 request per hour or so.
;; Additionally, it keeps a list of user who has successfully requested the faucet so they cannot
;; invoke the service again unless they are admins.
;; Users and setup is persisted on a file in disk.

(def discord (nodejs/require "discord.js"))
(nodejs/require "discord-reply")
(def Client (.-Client discord))
(def Https (nodejs/require "https"))
(def Http (nodejs/require "http"))
(def url (nodejs/require "url"))

(defonce status (atom :enabled))
;; list of limits: (time-window-in-seconds max-requests)
(defonce limits (atom [[60 2]
                       [600 5]
                       [3600 10]]))
(defonce requests-timestamps (atom (vector)))
(defonce users (atom (set [])))


(defn load-data [persistence-file]
  (let [{persisted-status :status persisted-limits :limits persisted-users :users} (reader/read-string (slurp persistence-file))]
    (when (some? persisted-status) (reset! status persisted-status))
    (when (some? persisted-limits) (reset! limits persisted-limits))
    (when (some? persisted-users) (reset! users (set persisted-users)))))


(defn persist-data [persistence-file]
  (try
    (spit persistence-file {:status @status
                            :limits @limits
                            :users @users})
    (catch :default e
      (log/error "Failed to persist faucet discord bot data" {:error e}))))


(defn reach-limits? []
  (let [now (now)
        max-time-window (apply max (map first @limits))]
    (swap! requests-timestamps (fn [requests] (filter #(> %1 (- now (* 1000 max-time-window))) requests)))
    (some (fn [[time-window max-requests]]
            (<= max-requests (count (filter #(> %1 (- now (* 1000 time-window))) @requests-timestamps))))
          @limits)))


(defn has-any-role? [message roles]
  (-> message (.-member) (.-roles) (.-cache) (.some (fn [role] (some #{(-> role (.-name))} roles)))))


(defn in-any-channel? [message channels]
  (let [channel (-> message (.-channel) (.-name))]
  (some #{channel} channels)))


(defn parse-command [message]
  (-> message (.-content) (string/split #" ") rest))


(defn on-success [message {:keys [:admin-roles]}]
  (when-not (has-any-role? message admin-roles)
    (let [author (-> message (.-author) (.-id))]
      (swap! users conj author)
      (swap! requests-timestamps conj (now)))))


(defn reply [message answer]
  (-> message (.lineReply answer)))


(defn bot-user [client]
  (str "@" (.. client -user -username)))


(defn usage-text [client]
  (str "To get DANK, make a tweet following the instructions at <https://memefactory.io/get-dank>\n"
       "Make sure you follow the proper tweet format, including a valid wallet address.\n"
       "Then copy the link to the tweet here mentioning " (.-user client) "\n"
       "For example: " (.-user client) " https://twitter.com/username/status/12345678901234567890"))


(defn send-faucet-request [client message tweet-url {:keys [:twitter-faucet-url] :as opts}]
  (try
    (let [data (atom "")
          post-data (js/JSON.stringify (clj->js {:tweet-url tweet-url}))
          url (.parse url twitter-faucet-url)
          options {:hostname (.-hostname url)
                   :port (.-port url)
                   :path (.-path url)
                   :method  "POST"
                   :headers {:Content-Type   "application/json"
                             :Content-Length (count post-data)}}
          http-client (if (= "http:" (.-protocol url)) Http Https)
          request (.request http-client (clj->js options)
                        (fn [resp]
                          (.on resp "data" (fn [c] (swap! data str c)))
                          (.on resp "end"
                             (fn []
                               (try
                                 (let [response (js->clj (js/JSON.parse @data) :keywordize-keys true)
                                       status (:status response)]
                                     (if (not= status "success")
                                       (let [error-message (:message response)]
                                         (if (= error-message "tweet format not valid")
                                           (reply message (str "Tweet format not valid. " (usage-text client)))
                                           (reply message error-message)))
                                       (do
                                         (reply message "DANK allotment requested. You should receive your DANK shortly")
                                         (on-success message opts)))
                                   (log/info (str response)))
                                 (catch js/Error e
                                   (log/error (str e))))))))]
      (.on request "error" (fn [e]
                             (log/error "Failed to request faucet" {:error e})
                             (reply message "Failed to process request")))
      (.write request post-data)
      (.end request))
    (catch :default e
      (log/error "Failed to request faucet" {:error e}))))


;; Commands
(defn command-help [client message]
  (reply message (str "DANK Faucet Bot Usage\n"
                      (bot-user client) " https://twitter.com/username/status/12345678901234567890 -> Send request to Faucet\n\n"
                      "Available commands:\n"
                      "!dank-faucet help -> show this help\n"
                      "!dank-faucet status -> show whether faucet is enabled\n"
                      "!dank-faucet start -> start faucet\n"
                      "!dank-faucet stop -> stop faucet\n"
                      "!dank-faucet shut -> like stop but silent\n"
                      "!dank-faucet clear-limits -> clear request history to refresh time windows\n"
                      "!dank-faucet get-limits -> show current limits\n"
                      "!dank-faucet set-limits -> set new usage limits\n"
                      )))

(defn command-status [message]
  (reply message (str "status: " (name @status))))

(defn command-start [message]
  (reset! status :enabled)
  (reply message "Faucet started"))

(defn command-stop [message]
  (reset! status :disabled)
  (reply message "Faucet stopped"))

(defn command-shut [message]
  (reset! status :shut)
  (reply message "Faucet shut"))

(defn command-clear-request-timestamps [message]
  (reset! requests-timestamps (vector))
  (reply message "Limits cleared"))

(defn command-get-limits [message]
  (reply message (str "limits ((time-window-1 max-request-1) (time-window-2 max-request-2) (...)): " @limits)))

(defn command-set-limits [message args]
  (if (or (= 1 (rem (count args) 2)) (some js/isNaN args))
    (reply message (str "Invalid limits format.\nUsage !dank-faucet set-limits time-window-1 max-request-1 time-window-2 max-request-2 ...\n"
                        "Example: !dank-faucet set-limits 60 2 600 5 3600 10"))
    (let [new-limits (partition 2 (map js/parseInt args))]
      (reset! limits new-limits)
      (reply message (str "New limits: " new-limits)))))

(defn command-unknown [message]
  (reply message "Unknown command. Type \"!dank-faucet help\" for a list of commands"))


;; On bot login
(defn on-login []
  (log/info "Dank Faucet Discord bot is online"))


;; Handle messages
(defn message-handler [client {:keys [admin-roles channels] :as opts}]
  (fn [message]
    (try
      ;; Ignore DM and messages whose author is the bot
      (if-not (or (= (.-author message) (.-user client)) (= (.. message -channel -type) "dm" ))
        (cond
          ;; Mentioned
          (and (or (-> message (.-mentions) (.has (.-user client))) (-> message (.-cleanContent) (.startsWith (bot-user client))))
               (or (empty? channels) (in-any-channel? message channels) ))
          (cond
            (= @status :shut) :do-nothing
            (not= @status :enabled) (reply message "Faucet temporarily disabled")
            (and (reach-limits?) (not (has-any-role? message admin-roles))) (reply message "Faucet is becoming very warm! Let it cool down for a few minutes")
            :else
              (let [tweet-url (first (re-find tweet-url-regex (-> message (.-cleanContent)) ))]
                (cond
                  (not tweet-url) (reply message (str "Invalid tweet URL. " (usage-text client)))
                  (and (not (has-any-role? message admin-roles)) (contains? @users (-> message (.-author) (.-id)))) (reply message "You have already requested faucet")
                  :else
                  (do
                      (reply message (str "Processing request..."))
                      (send-faucet-request client message tweet-url opts)
                      (log/info (str "Faucet requested by user: " (-> message (.-author) (.-username)) ": " (-> message (.-cleanContent))))))))

          ;; command-like
          (and (-> message (.-content) (.startsWith "!dank-faucet ")) (has-any-role? message admin-roles))
          (let [[command & args] (parse-command message)]
            (case command
              "help" (command-help client message)
              "status" (command-status message)
              "start" (command-start message)
              "stop" (command-stop message)
              "shut" (command-shut message)
              "clear-limits" (command-clear-request-timestamps message)
              "get-limits" (command-get-limits message)
              "set-limits" (command-set-limits message args)
              (command-unknown message)))))
    (catch :default e
      (log/error "Failed to process message" {:error e})))))


(defn load-persistence-data [persistence-file]
  (try
    (if (-> persistence-file File. .exists)
      (load-data persistence-file)
      (log/info (str "Faucet discord bot persistence file does not exists " persistence-file)))
    (doall (map (fn [atomref]
                  (add-watch atomref (keyword atomref)
                             #(persist-data persistence-file))
                  atomref)
                [status limits users]))
    (catch :default e
      (log/error "Failed to load faucet discord bot persisted data" {:error e}))))


(defn start [{:keys [:token :twitter-faucet-url :admin-roles :persistence-file :channels] :as opts}]
  (let [watchers (load-persistence-data persistence-file)
        client (Client.)]
    (.on client "ready" on-login)
    (.on client "message" (message-handler client opts))
    (.login client token)
    {:client client
     :watchers watchers}))

(defn stop [faucet]
  (.destroy (:client @faucet))
  (doseq [atomref [status limits users]]
             (remove-watch atomref (keyword atomref))))

(defstate dank-faucet-discord-bot
          :start (start (merge (:dank-faucet-discord-bot @config/config)
                               (:dank-faucet-discord-bot (mount/args))))
          :stop (stop dank-faucet-discord-bot))
