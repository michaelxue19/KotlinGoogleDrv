//
//  TubiSecurity.swift
//  MPP_IOS
//
//  Created by mxue on 10/2/19.
//  Copyright © 2019 mxue. All rights reserved.
//

import Foundation
import SwiftyRSA
import Gzip
import SharedKotlin

class SwiftIosBridge : IosBridge{

    func signWithSHA256(pemKey: String, message: String) -> String {
        do {
            let privateKeySwifty = try PrivateKey(pemEncoded: pemKey)
            let messageData = try ClearMessage(string: message, using: .utf8)
            let signature = try messageData.signed(with: privateKeySwifty, digestType: .sha256)
            print("signWithSHA256 returned signature successfully")
            return signature.base64String
        }catch{
            print(error)
        }
        return ""
    }
    
    func base64URLEncodedString(string: String)-> String{
        return Data(string.utf8).base64EncodedString()
        .replacingOccurrences(of: "/", with: "_")
        .replacingOccurrences(of: "+", with: "-")
        .replacingOccurrences(of: "=", with: "")
    }
    
    func currentMillisecondsSince1970() -> Int64 {
        return Date().millisecondsSince1970
    }
    
    func gzip(data string: String) -> KotlinByteArray {
        let compressedData: Data = try! (string.data(using: .utf8)?.gzipped())!
        
        let kotlinByteArray: KotlinByteArray = KotlinByteArray.init(size: Int32(compressedData.count))
        
        for (index, element) in compressedData.enumerated() {
            kotlinByteArray.set(index: Int32(index), value: Int8(element))
        }
        
        return kotlinByteArray
    }
}
