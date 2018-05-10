package com.nibiru.evilap

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.*

// https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0
// https://josiassena.com/eventbus-with-rxjava2-using-kotlin/
// https://lorentzos.com/rxjava-as-event-bus-the-right-way-10a36bdd49ba
// Singleton class to send2BackEnd events
enum class RxEventBus {
    INSTANCE;

    // the actual publishers/subscribers handling all the events
    private val busUi = PublishSubject.create<Any>()
    private val busService = PublishSubject.create<Any>()

    fun send2FrontEnd(o: Any) { busUi.onNext(o) }
    fun send2BackEnd(o: Any) { busService.onNext(o) }
    fun getFrontEndObservable(): Observable<Any> = busUi
    fun getBackEndObservable(): Observable<Any> = busService


}

