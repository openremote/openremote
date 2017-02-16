//
//  CustomURLProtocol.swift
//  console
//
//  Created by William Balcaen on 16/02/17.
//  Copyright © 2017 TInSys. All rights reserved.
//

import Foundation
class CustomURLProtocol: URLProtocol, URLSessionDataDelegate, URLSessionTaskDelegate {
    private var dataTask: URLSessionDataTask?
    private var urlResponse: URLResponse?
    private var receivedData: NSMutableData?
    public static var accessToken : String?
    class var CustomHeaderSet: String {
        return "CustomHeaderSet"
    }
    
    // MARK: NSURLProtocol
    
    override class func canInit(with request: URLRequest) -> Bool {
        guard let host = request.url?.host, host == "192.168.99.100" else {
            return false
        }
        if (URLProtocol.property(forKey: CustomURLProtocol.CustomHeaderSet, in: request as URLRequest) != nil) {
            return false
        }
        
        return true
    }
    
    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        return request
    }
    
    override func startLoading() {
        
        let mutableRequest =  NSMutableURLRequest.init(url: self.request.url!, cachePolicy: NSURLRequest.CachePolicy.useProtocolCachePolicy, timeoutInterval: 240.0)
        
        //Add Custom headers
        
        mutableRequest.setValue(String(format: "Bearer %@",CustomURLProtocol.accessToken!), forHTTPHeaderField: "Authorization")
        mutableRequest.setValue("master", forHTTPHeaderField: "Auth-Realm")
        URLProtocol.setProperty("true", forKey: CustomURLProtocol.CustomHeaderSet, in: mutableRequest)
        let defaultConfigObj = URLSessionConfiguration.default
        let defaultSession = URLSession(configuration: defaultConfigObj, delegate: self, delegateQueue: nil)
        self.dataTask = defaultSession.dataTask(with: mutableRequest as URLRequest)
        self.dataTask!.resume()
        
    }
    
    override func stopLoading() {
        self.dataTask?.cancel()
        self.dataTask       = nil
        self.receivedData   = nil
        self.urlResponse    = nil
    }
    
    // MARK: NSURLSessionDataDelegate
    
    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask,
                    didReceive response: URLResponse,
                    completionHandler: @escaping (URLSession.ResponseDisposition) -> Void) {
        
        self.client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        
        self.urlResponse = response
        self.receivedData = NSMutableData()
        
        completionHandler(.allow)
    }
    
    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
        self.client?.urlProtocol(self, didLoad: data as Data)
        
        self.receivedData?.append(data as Data)
    }
    
    // MARK: NSURLSessionTaskDelegate
    
    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if error != nil { 
            self.client?.urlProtocol(self, didFailWithError: error!)
        } else {
            self.client?.urlProtocolDidFinishLoading(self)
        }
    }
}
