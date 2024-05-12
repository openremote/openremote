# MQTT Gateway Handler
The MQTT Gateway Handler is a publish subscribe API providing a full suite of asset management controls and event subscriptions. 

The authentication requires a 'Service User' username and secret and is done through using the standard MQTT username 
and password mechanism to connect to the OpenRemote MQTT Broker.
- Host: Host of the manager (e.g., ```demo.openremote.io```)
- Port: 8883 for SSL or 1883 if without.
- Encryption/TLS: true when using SSL or false if not.
- Username: ```{realm}:{username}```
- Password: ```{secret}```
- ClientId: Anything you like, don't use the same ClientId it more than once.

#### Notes
- You need to use a Service User and not a regular user.
- The provided ClientId must also be present in the clientId topic matches.
- The MQTT Gateway API provides additional functionality when using a 'Gateway Asset V2' generated Service User.
Using a Service User associated with a Gateway Asset will ensure that all operations are performed on behalf of the Gateway Asset.
An example of this is when creating an asset, the asset will be created under the Gateway Asset.

### Last will publishing
Clients can configure a last will topic and payload as defined in the MQTT specification; the topic and payload can use the standard attribute 
publish topic/payload so it is possible to update an attribute when the client connection is closed un-expectedly; 
the client must have permission to access to the specified attribute.

***
## MQTT API Specification

### Operations (Publish)
Operations are publish topics that provide Asset management functionality, 
each operation topic has an associated *response* topic that can be subscribed to which will receive a success or error response.

#### Notes:
- Each operations topic has corresponding response topic suffix, this can be subscribed to beforehand. 
Example:```{realm}/{clientId}/operations/assets/{responseIdentifier}/create/response```
- Payloads will always use JSON encoding.
- Using a Gateway Asset V2 Service User will ensure that all operations are performed on behalf of the Gateway Asset.
Meaning the Gateway Asset is the parent of all assets created by the Service User.

##### [Assets](https://github.com/openremote/openremote/blob/master/model/src/main/java/org/openremote/model/asset/Asset.java)
- ```{realm}/{clientId}/operations/assets/{responseIdentifier}/create``` 
Create an asset. Requires a valid [asset template]() as the payload.
- ```{realm}/{clientId}/operations/assets/{assetId}/get```
Request the data of the specified assetId.
- ```{realm}/{clientId}/operations/assets/{assetId}/update```
Updates the specified asset. Requires a valid [update template]() as the payload.
- ```{realm}/{clientId}/operations/assets/{assetId}/delete```
Deletes the specified asset.

##### [Attributes](https://github.com/openremote/openremote/blob/master/model/src/main/java/org/openremote/model/attribute/Attribute.java)
- ```{realm}/{clientId}/operations/assets/{assetId}/attributes/{attributeName}/update``` 
Updates the specified attribute of the specified asset.
- ```{realm}/{clientId}/operations/assets/{assetId}/attributes/update```
Updates the attributes of the specified asset based on the payload, allows for multi-attribute updating.
- ```{realm}/{clientId}/operations/assets/{assetId}/attributes/get```
Request the attribute data of the specified asset.
- ```{realm}/{clientId}/operations/assets/{assetId}/attributes/{attributeName}/get```
Request the specified attribute value of the specified asset.

### Events (Subscribing)
Events are subscription topics that allow for subscribing to various events such as new Assets 
being created or updates and attribute values being changed. 
Subscription events allow filtering through the usage of MQTT wildcard masks (+ and #).

### Notes:
- The response from the subscriptions are encoded in JSON.
- Using a Gateway Asset V2 Service User will enforce the Gateway Asset as the parent, 
so the filters will be relative to the Gateway Asset rather than the realm.

##### [AssetEvent](https://github.com/openremote/openremote/blob/master/model/src/main/java/org/openremote/model/asset/AssetEvent.java)
- ```{realm}/{clientId}/events/assets/#```
Subscribes to all Asset Events of the realm.
- ```{realm}/{clientId}/events/assets/+```
Subscribes to all Asset events for direct children of the realm.
- ```{realm}/{clientId}/events/assets/{assetId}```
Subscribes to all Asset events for the specified asset.
- ```{realm}/{clientId}/events/assets/{assetId}/#```
Subscribes to all Asset events for the descendants of the specified asset.
- ```{realm}/{clientId}/events/assets/{assetId}/+```
Subscribes to all Asset events for the direct children of the specified asset.

##### [AttributeEvent](https://github.com/openremote/openremote/blob/master/model/src/main/java/org/openremote/model/attribute/AttributeEvent.java)
- ```{realm}/{clientId}/events/assets/+/attributes/#```
Subscribes to all Attribute Events of the realm.
- ```{realm}/{clientId}/events/assets/+/attributes/+```
Subscribes to all Attribute events for direct children of the realm.
- ```{realm}/{clientId}/events/assets/+/attributes/{attributeName}/#```
All attribute events for the specified attribute name.
- ```{realm}/{clientId}/events/assets/+/attributes/{attributeName}/+```
All attribute events for direct children with the specified attribute name.
- ```{realm}/{clientId}/events/assets/{assetId}/attributes/+```
Subscribes to all Attribute events for the direct children of the specified asset.
- ```{realm}/{clientId}/events/assets/{assetId}/attributes/{attributeName}```
All attribute events for the specified asset with the specified attribute name.
- ```{realm}/{clientId}/events/assets/{assetId}/attributes/{attributeName}/#```
All attribute events for descendants of the specified asset with the specified attribute name.
- ```{realm}/{clientId}/events/assets/{assetId}/attributes/{attributeName}/+```
All attribute events for direct children of the specified asset with the specified attribute name.


### Device Provisioning
Device provisioning is a process that allows for the automatic provisioning of Assets, using X.509 certificates.