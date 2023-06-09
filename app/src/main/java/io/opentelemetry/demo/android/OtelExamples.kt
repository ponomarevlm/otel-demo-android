package io.opentelemetry.demo.android

import android.annotation.SuppressLint
import android.util.Log
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.demo.android.tracing.error
import io.opentelemetry.demo.android.tracing.span
import io.opentelemetry.demo.android.tracing.spanStart
import io.opentelemetry.demo.android.tracing.tong
import io.opentelemetry.instrumentation.okhttp.v3_0.OkHttpTelemetry
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.http.GET
import toothpick.ktp.KTP
import toothpick.ktp.delegate.inject

/**
 * @author Ponomarev Leonid (leonid.phoenix@gmail.com)
 * @since 17.03.2023
 */
class OtelExamples {
    private val tracer: Tracer by inject()
    private val telemetry: OpenTelemetry by inject()

    init {
        KTP.openRootScope().inject(this)
    }

    interface ApiSpec {
        @GET("/search?q=opentelemetry+android+example")
        fun query(): Completable

        @GET("/404")
        fun error(): Completable
    }

    private val api: ApiSpec =
        Retrofit.Builder()
            .baseUrl("https://google.com")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .callFactory(
                OkHttpTelemetry.builder(telemetry).build()
                    .newCallFactory(OkHttpClient.Builder().build())
            )
            .build()
            .create(ApiSpec::class.java)

    fun run() {
        sync()
        rxSync()
        rxAsync()
        retrofit()
    }

    fun sync() {
        tracer.span("sync") {
            try {
                Thread.sleep(150)
                tracer.span("sync.subspan.1") {
                    Thread.sleep(100)
                }
                tracer.span("sync.subspan.2") {
                    Thread.sleep(200)
                }
                tracer.span("sync.subspan.3") {
                    Thread.sleep(50)
                    throw RuntimeException("sync error")
                }
            } catch (t: Throwable) {
                tracer.error(t)
            }
        }
    }

    @SuppressLint("CheckResult")
    fun rxSync() {
        tracer.span("rx.sync") {
            Observable.just("label")
                .doOnNext {
                    tracer.span("rx.sync.subspan.1") {
                        Thread.sleep(250)
                    }
                }
                .flatMapSingle {
                    Single.fromCallable {
                        tracer.span("rx.sync.subspan.2") {
                            Thread.sleep(50)
                        }
                    }
                }
                .subscribe({
                    tracer.span("rx.sync.subspan.3") {
                        Thread.sleep(250)
                    }
                    throw RuntimeException("rx.sync exception")
                })
                { throwable: Throwable ->
                    Log.w(TAG, "", throwable)
                    tracer.error(throwable)
                }
        }
    }

    @SuppressLint("CheckResult")
    fun rxAsync() {
        tracer.spanStart("rx.scheduled") { root ->
            val obs = Observable.create<Unit> { emitter ->
                emitter.onNext(Unit)
                emitter.onNext(Unit)
                emitter.onError(RuntimeException("rx.scheduled exception"))
            }
            obs
                .doOnNext {
                    tracer.tong("rx.scheduled.subspan.1", root) {
                        Thread.sleep(250)
                    }
                }
                .doOnNext {
                    tracer.tong("rx.scheduled.subspan.2", root) {
                        Thread.sleep(150)
                    }
                }
                .flatMapSingle {
                    Single.fromCallable {
                        tracer.span("rx.scheduled.subspan.3", root) {
                            Thread.sleep(50)
                        }
                    }
                }
                .doOnTerminate { root.end() }
                .subscribeOn(Schedulers.io())
                .subscribe(
                    {
                        tracer.tong("rx.scheduled.subspan.subscribe", root) {
                            Thread.sleep(150)
                        }
                    },
                    { throwable ->
                        tracer.error(throwable)
                        Log.w(TAG, "", throwable)
                    }
                )
        }

    }

    @SuppressLint("CheckResult")
    fun retrofit() {
        tracer.spanStart("retrofit") {
            api.query()
                .andThen(api.error())
                .doOnTerminate { it.end() }
                .subscribe({/* ignore */ }, {/* swallow */ })
        }
    }

    companion object {
        const val TAG = "OTelExamples"
    }
}