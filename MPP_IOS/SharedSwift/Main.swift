//
//  Main.swift
//  MPP_IOS
//
//  Created by mxue on 10/2/19.
//  Copyright © 2019 mxue. All rights reserved.
//

import Foundation
import SharedKotlin

let tubiRxBridge = TubiRxObjectBridge()
let tubiiosBridge = SwiftIosBridge()

func startWaterfall(){
    print("startWaterfall")
    IosPlatformKt.setIOSBridge(bridge: tubiiosBridge)
    RxWrapperKt.setSwiftRxBridge(bridge: tubiRxBridge)
    
    WaterfallEntry().start(cloudFolderName: "myios-1234")
}
