/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import Foundation
import UIKit

public enum ORServer {
    public static var scheme = "https"
    public static var hostURL = "openremote.io"
    public static var initialPath = "console/\(ORServer.realm)/"
    public static var navigationPath = "console/\(ORServer.realm)/"
    public static var realm = "openremote"
    public static var baseUrl = "\(ORServer.scheme)://\(ORServer.hostURL)/"
    public static var apiTestResource = String(format:"\(ORServer.scheme)://%@/%@/notification/alert",ORServer.hostURL,ORServer.realm)
    public static var errorUrl = ""
    public static var ignorePageErrors = false

    public static var registerDeviceResource = String(format:"\(ORServer.scheme)://%@/%@/notification/token",ORServer.hostURL,ORServer.realm)
    public static var deleteNotifiedAlertResource = String(format:"\(ORServer.scheme)://%@/%@/notification/alert/",ORServer.hostURL,ORServer.realm)
}

public enum ORClient {
    public static var clientId = "openremote"
}

public enum ORFCM {
    public static var serverKey = "Dummy"
}

public enum ORAppGroup {
    public static var entitlement = "group.org.openremote.console"
}

extension UIColor {
    public static var mainColor = UIColor(red:0.22, green:0.30, blue:0.31, alpha:1.0)
    public static var mainColorDark = UIColor(red:0.15, green:0.20, blue:0.22, alpha:1.0)
    public static var mainColorAccent = UIColor(red:0.75, green:0.84, blue:0.18, alpha:1.0)
}

