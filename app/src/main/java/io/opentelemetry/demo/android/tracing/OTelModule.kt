package io.opentelemetry.demo.android.tracing

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.demo.android.BuildConfig
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.instrumentation.rxjava.v2_0.TracingAssembly
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import toothpick.config.Module
import toothpick.ktp.binding.bind
import java.util.concurrent.TimeUnit


/**
 * @author Ponomarev Leonid (leonid.phoenix@gmail.com)
 * @since 01.01.2023
 */
class OTelModule : Module() {
    init {
        TracingAssembly.builder().setCaptureExperimentalSpanAttributes(true).build().enable()
        SchedulingTracingAssembly.enable()
        val resource: Resource = Resource.getDefault()
            .merge(
                Resource.create(
                    Attributes.of(ResourceAttributes.SERVICE_NAME, "io.opentelemetry.demo")
                )
            )

        val jaegerExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint("http://192.168.0.102:4317")
            .setTimeout(1, TimeUnit.SECONDS)
            .build()

        val sdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(
                if (BuildConfig.DEBUG)
                    SimpleSpanProcessor.create(jaegerExporter)
                else
                    BatchSpanProcessor.builder(jaegerExporter).build()
            )
            .setResource(resource)
            .build()

//        val sdkMeterProvider = SdkMeterProvider.builder()
//            .registerMetricReader(
//                PeriodicMetricReader.builder(
//                    OtlpGrpcMetricExporter.builder()
//                        .setEndpoint("http://192.168.0.102:4317")
//                        .setTimeout(1, TimeUnit.SECONDS)
//                        .build()
//                ).build()
//            )
//            .setResource(resource)
//            .build()

        val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
//            .setMeterProvider(sdkMeterProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal()
        val tracer = openTelemetry.getTracer("io.opentelemetry.demo.android")
        bind<OpenTelemetry>().toInstance(openTelemetry)
        bind<Tracer>().toInstance(tracer)
    }
}

fun Tracer.span(name: String, block: (span: Span) -> Unit) {
    spanBuilder(name).startSpan().also {
        it.makeCurrent().use { _ ->
            block.invoke(it)
        }
        it.end()
    }
}

fun Tracer.tong(name: String, block: (span: Span) -> Unit) = span(name, block) // egg

fun Tracer.error(throwable: Throwable) {
    span("error") {
        it.setStatus(StatusCode.ERROR, "${throwable.message}")
            .recordException(throwable)
    }
}
