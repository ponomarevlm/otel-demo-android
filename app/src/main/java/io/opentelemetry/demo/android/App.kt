package io.opentelemetry.demo.android

import android.app.Application
import android.util.Log
import com.akaita.java.rxjava2debug.RxJava2Debug
import io.opentelemetry.demo.android.tracing.OTelModule
import io.reactivex.plugins.RxJavaPlugins
import toothpick.ktp.KTP

/**
 * @author Ponomarev Leonid (leonid.phoenix@gmail.com)
 * @since 20.03.2023
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        RxJavaPlugins.setErrorHandler { t: Throwable ->
            Log.e("RxJava", "unhandled fatal", t)
        }

        // order matters, RxJava2Debug doesn't compose any chain of responsibility, set it before
        // other safe rxjava plugins
        RxJava2Debug.enableRxJava2AssemblyTracking(arrayOf(applicationContext.packageName))
        KTP.openRootScope()
            .installModules(OTelModule())
            .inject(this)
    }
}