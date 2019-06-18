package com.gab.gabby

/**
 * Created by charlag on 3/7/18.
 */

class FakeGabbyApplication : GabbyApplication() {

    private lateinit var locator: ServiceLocator

    override fun initSecurityProvider() {
        // No-op
    }

    override fun initAppInjector() {
        // No-op
    }

    override fun getServiceLocator(): ServiceLocator {
        return locator
    }
}