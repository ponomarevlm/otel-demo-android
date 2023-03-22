package io.opentelemetry.demo.android.tracing;

import org.reactivestreams.Subscriber;

import java.util.concurrent.atomic.AtomicBoolean;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * @author Ponomarev Leonid (leonid.phoenix@gmail.com)
 * @since 16.03.2023
 */
@SuppressWarnings("rawtypes")
public final class SchedulingTracingAssembly {
    static final AtomicBoolean lock = new AtomicBoolean();
    private static Function<? super Single, ? extends Single> onSingleAssemblySource;
    private static Function<? super Completable, ? extends Completable> onCompletableAssemblySource;
    private static Function<? super Maybe, ? extends Maybe> onMaybeAssemblySource;
    private static Function<? super Flowable, ? extends Flowable> onFlowableAssemblySource;
    private static Function<? super Observable, ? extends Observable> onObservableAssemblySource;

    private SchedulingTracingAssembly() {throw new IllegalStateException("singleton");}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void enable() {
        if (lock.compareAndSet(false, true)) {

            onCompletableAssemblySource = RxJavaPlugins.getOnCompletableAssembly();
            RxJavaPlugins.setOnCompletableAssembly(
                compose(
                    onCompletableAssemblySource,
                    completable -> new CompletableOnAssemblyScheduled(Context.current(), completable)
                )
            );

            onMaybeAssemblySource = RxJavaPlugins.getOnMaybeAssembly();
            RxJavaPlugins.setOnMaybeAssembly(
                compose(onMaybeAssemblySource,
                    maybe -> new MaybeOnAssemblyScheduled(Context.current(), maybe)
                )
            );

            onSingleAssemblySource = RxJavaPlugins.getOnSingleAssembly();
            RxJavaPlugins.setOnSingleAssembly(
                compose(
                    onSingleAssemblySource,
                    single -> new SingleOnAssemblyScheduled(Context.current(), single)
                )
            );

            onObservableAssemblySource = RxJavaPlugins.getOnObservableAssembly();
            RxJavaPlugins.setOnObservableAssembly(
                compose(
                    onObservableAssemblySource,
                    observable -> new ObservableOnAssemblyScheduled(Context.current(), (observable))
                )
            );

            onFlowableAssemblySource = RxJavaPlugins.getOnFlowableAssembly();
            RxJavaPlugins.setOnFlowableAssembly(
                compose(
                    onFlowableAssemblySource,
                    flowable -> new FlowableOnAssemblyScheduled<>(Context.current(), (flowable))
                )
            );
        }
    }

    public static void disable() {
        if (lock.compareAndSet(false, true)) {
            RxJavaPlugins.setOnCompletableAssembly(onCompletableAssemblySource);
            RxJavaPlugins.setOnSingleAssembly(onSingleAssemblySource);
            RxJavaPlugins.setOnMaybeAssembly(onMaybeAssemblySource);
            RxJavaPlugins.setOnObservableAssembly(onObservableAssemblySource);
            RxJavaPlugins.setOnFlowableAssembly(onFlowableAssemblySource);

            lock.set(false);
        }
    }

    private static <R> Function<? super R, ? extends R> compose(
        Function<? super R, ? extends R> before,
        Function<? super R, ? extends R> after) {
        if (before == null) {
            return after;
        }
        return (R v) -> after.apply(before.apply(v));
    }

    private static class SingleOnAssemblyScheduled<T> extends Single<T> {
        final Context context;
        final SingleSource<T> source;

        SingleOnAssemblyScheduled(Context context, SingleSource<T> source) {
            this.context = context;
            this.source = source;
        }

        @Override
        protected void subscribeActual(SingleObserver<? super T> observer) {
            try (Scope ignore = context.makeCurrent()) {
                source.subscribe(observer);
            }
        }
    }

    private static class ObservableOnAssemblyScheduled<T> extends Observable<T> {
        final Context context;
        final ObservableSource<T> source;

        ObservableOnAssemblyScheduled(Context context, ObservableSource<T> source) {
            this.context = context;
            this.source = source;
        }

        @Override
        protected void subscribeActual(Observer<? super T> observer) {
            try (Scope ignore = context.makeCurrent()) {
                source.subscribe(observer);
            }
        }
    }

    private static class CompletableOnAssemblyScheduled extends Completable {
        final Context context;
        final CompletableSource source;

        CompletableOnAssemblyScheduled(Context context, CompletableSource source) {
            this.context = context;
            this.source = source;
        }

        @Override
        protected void subscribeActual(CompletableObserver observer) {
            try (Scope ignore = context.makeCurrent()) {
                source.subscribe(observer);
            }
        }
    }

    private static class MaybeOnAssemblyScheduled<T> extends Maybe<T> {
        final Context context;
        final MaybeSource<T> source;

        MaybeOnAssemblyScheduled(Context context, MaybeSource<T> source) {
            this.context = context;
            this.source = source;
        }

        @Override
        protected void subscribeActual(MaybeObserver<? super T> observer) {
            try (Scope ignore = context.makeCurrent()) {
                source.subscribe(observer);
            }
        }
    }

    private static class FlowableOnAssemblyScheduled<T> extends Flowable<T> {
        final Context context;
        final Flowable<T> source;

        FlowableOnAssemblyScheduled(Context context, Flowable<T> source) {
            this.context = context;
            this.source = source;
        }

        @Override
        protected void subscribeActual(Subscriber<? super T> observer) {
            try (Scope ignore = context.makeCurrent()) {
                source.subscribe(observer);
            }
        }
    }
}
