# Open Telemetry tracing android integration example

Open Telemetry android integration and usage example bundled with fixes for OTel/RxJava 2 ([reason](https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/8144)) & Retrofit 3/RxJava2 ([reason](https://github.com/square/retrofit/pull/3867)).

App includes tracing examples from trivial sync ones to RxJava schedulers compatible
![image](https://user-images.githubusercontent.com/54063/228151219-00bf2244-c3fc-4bda-9f55-90e9f279274b.png)

and chained Retrofit/rx calls
![image](https://user-images.githubusercontent.com/54063/228151178-a6c704e3-119f-4af1-90e3-1502720364f6.png)


## Visualisation on local tracing backend
It's proposed to use Jaeger for it's one-line install.

<https://www.jaegertracing.io/docs/1.43/getting-started/>
	
	docker run -d --name jaeger \
	  -e COLLECTOR_ZIPKIN_HOST_PORT=:9411 \
	  -e COLLECTOR_OTLP_ENABLED=true \
	  -p 6831:6831/udp \
	  -p 6832:6832/udp \
	  -p 5778:5778 \
	  -p 16686:16686 \
	  -p 4317:4317 \
	  -p 4318:4318 \
	  -p 14250:14250 \
	  -p 14268:14268 \
	  -p 14269:14269 \
	  -p 9411:9411 \
	  jaegertracing/all-in-one:1.43


## Integrations steps

Follow second commit changes

* copy all retrofit2.adapter.rxjava2 package files as is preserving paths
* all other additions place regarding your projects conventions
* [dependencies](./app/build.gradle#L80)
* [disable stock retrofit/rx adapter](./app/build.gradle#L77) to use bundled one
* [add exclusion statements for each firebase component](./app/build.gradle#L53)
* local tracing backend host
	* [specify](./app/src/main/java/io/opentelemetry/demo/android/tracing/OTelModule.kt#L40)
	* [specify net config](./app/src/main/AndroidManifest.xml#L20)
	* [configure whitelist](./app/src/main/res/xml/net_security_config.xml)
* [pay attention to RxJava plugins init order](./app/src/main/java/io/opentelemetry/demo/android/App.kt#L23). First should go 'destructive' one, like RxJava2Debug, they just set their own hooks, overriding any others, so there's no point use more than one. Then in any order ['safe'](./app/src/main/java/io/opentelemetry/demo/android/tracing/OTelModule.kt#L31) may be used, they add their hooks [via composition with existing](./app/src/main/java/io/opentelemetry/demo/android/tracing/SchedulingTracingAssembly.java#L46)
* [place configured deps into DI container](./app/src/main/java/io/opentelemetry/demo/android/App.kt#L27), they may be global singletons, it's normal usecase
* [configure retrofit](./app/src/main/java/io/opentelemetry/demo/android/OtelExamples.kt#L48)
* [use as in sample](./app/src/main/java/io/opentelemetry/demo/android/OtelExamples.kt#L54)


