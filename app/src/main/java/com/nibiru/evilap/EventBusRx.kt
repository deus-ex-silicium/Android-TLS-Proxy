package com.nibiru.evilap

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

// https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0
// https://josiassena.com/eventbus-with-rxjava2-using-kotlin/
// Singleton class to send events
enum class EventBusRx {
    INSTANCE;

    // the actual publisher handling all of the events
    private val bus = PublishSubject.create<EvilApService.Host>()
    // the message being sent to all subscribers
    fun send(host: EvilApService.Host) {
        bus.onNext(host)
    }
    // return the publisher itself as an observable to subscribe to
    fun toObserverable(): Observable<EvilApService.Host> {
        return bus
    }
}