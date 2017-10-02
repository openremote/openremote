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

enum Server {
    static let scheme = "https"
    // static let hostURL = "192.168.1.145:8080"
    static let hostURL = "blok61.openremote.io"
    static let initialPath = "console/\(Server.realm)/"
    static let navigationPath = "console/\(Server.realm)/"
    static let realm = "blok61"
    static let apiTestResource = String(format:"\(Server.scheme)://%@/%@/notification/alert",Server.hostURL,Server.realm)

    static let registerDeviceResource = String(format:"\(Server.scheme)://%@/%@/notification/token",Server.hostURL,Server.realm)
    static let deleteNotifiedAlertResource = String(format:"\(Server.scheme)://%@/%@/notification/alert/",Server.hostURL,Server.realm)
}

enum Client {
    static let clientId = "openremote"
}

enum FCM {
    static let serverKey = "Dummy"
}

enum AppGroup {
    static let entitlement = "group.org.openremote.console"
}
