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

}
