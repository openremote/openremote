import WebKit

extension WKWebView {
    
    enum PrefKey {
        static let cookie = "cookies"
    }
    
    func writeCookiesToStorage(for domain: String, completion: (() -> (Void))?) {
        fetchInMemoryCookies(for: domain) { cookies in
            UserDefaults.standard.set(cookies, forKey: PrefKey.cookie + domain)
            completion?();
        }
    }
    
    func clearCookies(for domain: String, completion: (() -> (Void))?) {
        WKWebsiteDataStore.default().fetchDataRecords(ofTypes: WKWebsiteDataStore.allWebsiteDataTypes()) { records in
                    records.forEach { record in
                        WKWebsiteDataStore.default().removeData(ofTypes: record.dataTypes, for: [record], completionHandler: {})
                        print("[WebCacheCleaner] Record \(record) deleted")
                    }
                }
        WKWebsiteDataStore.default().httpCookieStore.getAllCookies { (cookies) in
            for cookie in cookies {
                if domain.contains(cookie.domain) {
                    self.configuration.websiteDataStore.httpCookieStore.delete(cookie)
                }
            }
        }
        UserDefaults.standard.removeObject(forKey: PrefKey.cookie + domain)
        UserDefaults.standard.synchronize()
        completion?();
    }
    
    
    func loadCookiesFromStorage(for domain: String, completion: (() -> (Void))?) {
        if let storedCookies = UserDefaults.standard.dictionary(forKey: (PrefKey.cookie + domain)){
            fetchInMemoryCookies(for: domain) { freshCookies in
                
                let mergedCookie = storedCookies.merging(freshCookies) { (_, new) in new }
                
                for (_, cookieConfig) in mergedCookie {
                    let cookie = cookieConfig as! Dictionary<String, Any>
                    
                    var expire : Any? = nil
                    
                    if let expireTime = cookie["Expires"] as? Double{
                        expire = Date(timeIntervalSinceNow: expireTime)
                    }
                    
                    let newCookie = HTTPCookie(properties: [
                        .domain: cookie["Domain"] as Any,
                        .path: cookie["Path"] as Any,
                        .name: cookie["Name"] as Any,
                        .value: cookie["Value"] as Any,
                        .secure: cookie["Secure"] as Any,
                        .expires: expire as Any
                    ])
                    
                    self.configuration.websiteDataStore.httpCookieStore.setCookie(newCookie!)
                }
                
                completion?()
            }
            
        }
        else{
            completion?()
        }
    }
    
    func fetchInMemoryCookies(for domain: String, completion: (([String: Any]) -> (Void))?) {
        var cookieDict = [String: AnyObject]()
        WKWebsiteDataStore.default().httpCookieStore.getAllCookies { (cookies) in
            for cookie in cookies {
                if domain.contains(cookie.domain) {
                    cookieDict[cookie.name] = cookie.properties as AnyObject?
                }
            }
            completion?(cookieDict)
        }
    }
}
