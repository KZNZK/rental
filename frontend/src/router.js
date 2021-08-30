
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import RentalManager from "./components/RentalManager"

import PaymentManager from "./components/PaymentManager"

import DeliveryManager from "./components/DeliveryManager"


import Mypage from "./components/Mypage"
export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [
            {
                path: '/rentals',
                name: 'RentalManager',
                component: RentalManager
            },

            {
                path: '/payments',
                name: 'PaymentManager',
                component: PaymentManager
            },

            {
                path: '/deliveries',
                name: 'DeliveryManager',
                component: DeliveryManager
            },


            {
                path: '/mypages',
                name: 'Mypage',
                component: Mypage
            },


    ]
})
