(ns firmata.test.i2c
  (:require #+clj 
            [clojure.test :as t
                   :refer (is deftest with-test run-tests testing)]
            #+cljs
            [cemerick.cljs.test :as t]
            [firmata.test.async-helpers :refer [get-event]]
            [firmata.test.mock-stream :as mock]
            [firmata.core :refer [open-board event-channel]]
            [firmata.i2c :refer [send-i2c-request send-i2c-config]])
  #+cljs 
  (:require-macros [cemerick.cljs.test
                       :refer (is deftest with-test run-tests testing test-var)]))

(deftest test-read-i2c-events

  (let [client   (mock/create-mock-stream)
        board    (open-board client)
        evt-chan (event-channel board)]

    (testing "read i2c-reply"
      (mock/receive-bytes client 0xF0 0x77
                                 0xA 0x0 ; slave address
                                 0x4 0x1 ; register
                                 0x68 0x7 ; data0
                                 0x01 0x0  ; data1
                                 0xF7)
      (get-event evt-chan (fn [event]
          (is (= :i2c-reply (:type event)))
          (is (= 0x0A (:slave-address event)))
          (is (= 0x84 (:register event)))
          (is (= [1000 1] (:data event))))))
    ))

(deftest test-i2c-messages
  (let [client (mock/create-mock-stream)
        board (open-board client)]

    (testing "ic2 request: write"
      (send-i2c-request board 6 :write 0xF 0xE 0xD)
      (is (= [0xF0 0x76 6 0 0xF 0x0 0xE 0 0xD 0x0 0xF7] (mock/last-write client))))

    (testing "ic2 request: read-once"
      (send-i2c-request board 6 :read-once 1000)
      (is (= [0xF0 0x76 6 2r0000100 0x68 0x7 0xF7] (mock/last-write client))))

    (testing "ic2 request: read-continuously"
      (send-i2c-request board 7 :read-continuously)
      (is (= [0xF0 0x76 7 2r1000 0xF7] (mock/last-write client))))

    (testing "ic2 request: stop-reading"
      (send-i2c-request board 7 :stop-reading)
      (is (= [0xF0 0x76 7 2r1100 0xF7] (mock/last-write client))))

    (testing "ic2 config: delay"
      (send-i2c-config board 1000)
      (is (= [0xF0 0x78 0x68 0x7 0xF7] (mock/last-write client))))

    (testing "ic2 config: delay and user data"
      (send-i2c-config board 1000 0x10)
      (is (= [0xF0 0x78 0x68 0x7 0x10 0xF7] (mock/last-write client))))

  ))
