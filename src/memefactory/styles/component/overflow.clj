(ns memefactory.styles.component.overflow)

(defn of-ellipsis []
  [:&
   {:white-space :nowrap
    :overflow :hidden
    :text-overflow :ellipsis}])
