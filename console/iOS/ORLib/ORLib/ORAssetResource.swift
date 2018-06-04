//
//  ORAssetResoure.swift
//  GoogleToolboxForMac
//
//  Created by Michael Rademaker on 04/06/2018.
//

import UIKit

open class ORAssetResoure: NSObject, URLSessionDelegate {

    public static let sharedInstance = ORAssetResoure()

    private override init() {
        super.init()
    }

    open func updateAssetAttribute(assetId : String, attributeName : String, rawJson : Data) {
        UIApplication.shared.isNetworkActivityIndicatorVisible = true
        TokenManager.sharedInstance.getAccessToken { (accessTokenResult) in
            switch accessTokenResult {
            case .Failure(let error) :
                UIApplication.shared.isNetworkActivityIndicatorVisible = false
                ErrorManager.showError(error: error!)
            case .Success(let accessToken) :
                self.updateAssetAttribute(assetId: assetId, attributeName: attributeName, accessToken: accessToken, rawJson: rawJson)
            }
        }
    }

    open func updatePublicAssetAttibute(assetId : String, attributeName : String, rawJson : Data) {
        updateAssetAttribute(assetId: assetId, attributeName: attributeName, accessToken: nil, rawJson: rawJson)
    }

    private func updateAssetAttribute(assetId : String, attributeName : String, accessToken :String?, rawJson : Data) {
        guard let urlRequest = URL(string: String(String(format: "\(ORServer.scheme)://%@/%@/asset/%@/attribute/%@", ORServer.hostURL, ORServer.realm, assetId, attributeName))) else { return }
        let request = NSMutableURLRequest(url: urlRequest)
        request.httpMethod = "PUT"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = rawJson
        if let token = accessToken {
            request.addValue(String(format:"Bearer %@", token), forHTTPHeaderField: "Authorization")
        }
        let sessionConfiguration = URLSessionConfiguration.default
        let session = URLSession(configuration: sessionConfiguration, delegate: self, delegateQueue : nil)
        let reqDataTask = session.dataTask(with: request as URLRequest, completionHandler: { (data, response, error) in
            DispatchQueue.main.async {
                UIApplication.shared.isNetworkActivityIndicatorVisible = false
                if (error != nil) {
                    NSLog("error %@", (error! as NSError).localizedDescription)
                    let error = NSError(domain: "", code: 0, userInfo:  [
                        NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                        ])
                    ErrorManager.showError(error: error)
                }
            }
        })
        reqDataTask.resume()
    }
}
