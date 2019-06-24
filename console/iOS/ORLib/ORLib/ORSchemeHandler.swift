//
//  ORSchemeHandler.swift
//  ORLib
//
//  Created by Michael Rademaker on 24/06/2019.
//

import UIKit
import WebKit

class ORSchemeHandler: NSObject, WKURLSchemeHandler {

    public static let browserScheme = "webbrowser"

    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        if let url = urlSchemeTask.request.url, url.scheme == ORSchemeHandler.browserScheme {
            if var components = URLComponents(url: url, resolvingAgainstBaseURL: false) {
                components.scheme = "https"
                if let newUrl = components.url {
                    UIApplication.shared.open(newUrl)
                }
            }
            urlSchemeTask.didReceive(URLResponse(url: url, mimeType: nil, expectedContentLength: -1, textEncodingName: nil))
            urlSchemeTask.didFinish()
        }
    }

    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {
        urlSchemeTask.didFailWithError(WKError(WKError.unknown))
    }
}
