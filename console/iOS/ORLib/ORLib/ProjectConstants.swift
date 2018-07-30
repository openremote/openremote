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

public enum ORServer {
    public static var scheme = "https"
    public static var hostURL = "openremote.io"
    public static var initialPath = "console/\(ORServer.realm)/"
    public static var navigationPath = "console/\(ORServer.realm)/"
    public static var realm = "openremote"
    public static var baseUrl = "\(ORServer.scheme)://\(ORServer.hostURL)/"
    public static var apiTestResource = String(format:"\(ORServer.scheme)://%@/%@/notification/alert",ORServer.hostURL,ORServer.realm)

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
