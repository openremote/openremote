//
//  String+Utils.swift
//  GenericApp
//
//  Created by Michael Rademaker on 26/10/2020.
//  Copyright © 2020 OpenRemote. All rights reserved.
//

import Foundation

extension String {

  func stringByURLEncoding() -> String? {

    let characters = CharacterSet.urlQueryAllowed.union(CharacterSet(charactersIn: "#"))

    guard let encodedString = self.addingPercentEncoding(withAllowedCharacters: characters) else {
      return nil
    }

    return encodedString
  }
    
    var isValidURL: Bool {
        let detector = try! NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue)
        if let match = detector.firstMatch(in: self, options: [], range: NSRange(location: 0, length: self.utf16.count)) {
            // it is a link, if the match covers the whole string
            return match.range.length == self.utf16.count
        } else {
            return false
        }
    }

}
