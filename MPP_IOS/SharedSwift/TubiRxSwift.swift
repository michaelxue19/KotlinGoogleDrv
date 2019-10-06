//
//  TubiRxSwift.swift
//  MPP_IOS
//
//  Created by mxue on 10/2/19.
//  Copyright © 2019 mxue. All rights reserved.
//

import Foundation

import SharedKotlin
import RxSwift

class Weak<T: AnyObject> {
  weak var value : T?
  init (value: T) {
    self.value = value
  }
}

//let mapTable = NSMapTable<String, Any>(valueOptions: .weakMemory)

var rxObjectMap = [Int32 : Any]()
var rxIndex: Int32 = 0

extension ObservableType {
    
    func debounce(_ fn: @escaping (Element) -> Completable) -> Observable<Element> {
        return flatMapLatest {
            fn($0).andThen(Observable.just($0))
        }
    }
}

extension Date {
    var millisecondsSince1970:Int64 {
        return Int64((self.timeIntervalSince1970 * 1000.0).rounded())
    }

    init(milliseconds:Int64) {
        self = Date(timeIntervalSince1970: TimeInterval(milliseconds) / 1000)
    }
}

class TubiRxObjectBridge : ITubiRxObjectBridge{

    private var backgroundWorkScheduler : OperationQueueScheduler
    let disposeBag = DisposeBag()
    
    init(){
        let operationQueue = OperationQueue()
        operationQueue.maxConcurrentOperationCount = 1
        operationQueue.qualityOfService = QualityOfService.userInitiated
        backgroundWorkScheduler = OperationQueueScheduler(operationQueue: operationQueue)
    }
    
    func createRxComplete(onSubscribe: @escaping () -> Void) -> Int32 {
        let rxSwiftComplete =  Completable.create { observer in
            onSubscribe()
            observer(.completed)
            return Disposables.create()
        }
        rxIndex += 1
        rxObjectMap[rxIndex] = rxSwiftComplete
        return rxIndex
    }
    
    func createRxEndless(getNext: @escaping () -> Any) -> Int32 {
        print("createRxEndless\n")
        let observable = Observable<Any>.create { (observer) in
            
            while (true) {
                let nextItem = getNext()
                observer.onNext(nextItem)
            }
            
            return Disposables.create()
        }
        rxIndex += 1
        rxObjectMap[rxIndex] = observable
        return rxIndex
    }
    
    func subscribeOn(handle: Int32, thread: Int32) {
        print("subscribeOn\n")
        var rxObject = rxObjectMap[handle]
        if let rxObservable = rxObject as? Observable<Any> {
            rxObject = rxObservable.subscribeOn(backgroundWorkScheduler)
        } else if let rxObservable = rxObject as? Completable {
            rxObject = rxObservable.subscribeOn(backgroundWorkScheduler)
        }
        rxObjectMap[handle] = rxObject
    }
    
    func subscribe(handle: Int32, onComplete: @escaping () -> Void) {
        print("subscribe\n")
        let rxObject = rxObjectMap[handle]
        if let rxObservable = rxObject as? Observable<Any> {
            rxObservable.subscribe({it in
                onComplete()
            }).disposed(by: disposeBag)
        } else if let rxCompletable = rxObject as? Completable {
            rxCompletable.subscribe(onCompleted:{
                onComplete()
            }).disposed(by: disposeBag)
        }
        //rxObjectMap[handle] = rxObject
    }
    
    func distinctUntilChanged(handle: Int32) {
        print("debounce\n")
    }
    
    func debounce(handle: Int32,debounceCheck: @escaping (_ t: Any) -> KotlinInt){
        print("debounce\n")
        var rxObject = rxObjectMap[handle]
        if let rxObservable = rxObject as? Observable<Any> {
            rxObject = rxObservable.debounce{ newItem in
                var debounceTimeInSeconds: Int = 0
                    debounceTimeInSeconds = Int(truncating: debounceCheck(newItem))
                    print(debounceTimeInSeconds)
                
                
                if(debounceTimeInSeconds <= 0){
                    return Completable.empty()
                }
                
                return Observable<Int>.timer(DispatchTimeInterval.seconds(debounceTimeInSeconds),
                        scheduler: MainScheduler.instance).ignoreElements()
            }
            rxObjectMap[handle] = rxObject
        }
    }
}
