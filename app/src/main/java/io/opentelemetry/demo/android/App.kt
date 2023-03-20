package io.opentelemetry.demo.android

import android.app.Application
import toothpick.ktp.KTP

/**
 * @author Ponomarev Leonid (leonid.phoenix@gmail.com)
 * @since 20.03.2023
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        KTP.openRootScope()
            .inject(this)
    }
}