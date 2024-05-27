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


## MQTT API Specification

### Operations (Publish)
Operations are publish topics that provide Asset management functionality, 
each operation topic has an associated **response** topic that can be subscribed to which will receive a success response or error response.

#### Notes:
- Each operations topic has corresponding **response topic** suffix, this can be subscribed to beforehand. 
> Response topic example:
> ```{realm}/{clientId}/operations/assets/{responseIdentifier}/create/response```
- Payloads will always use JSON encoding.
- Using a **Gateway Asset V2 Service User** will ensure that all operations are performed on behalf of the Gateway Asset.
This Service User can only manage assets part of the Gateway Asse hierarchy.

##### [Assets](https://github.com/openremote/openremote/blob/master/model/src/main/java/org/openremote/model/asset/Asset.java)
- ```{realm}/{clientId}/operations/assets/{responseIdentifier}/create``` 
Creates an asset, requires a valid [asset template](#asset-templates) as the payload. The response identifier is used to correlate the response to the request, requires a subscription to the response topic to receive the response.
- ```{realm}/{clientId}/operations/assets/{assetId}/get```
Request the data of the specified assetId, requires subscription to the response topic to receive the data.
- ```{realm}/{clientId}/operations/assets/{assetId}/update```
Updates the specified asset, requires a valid [asset template](#asset-templates) as the payload.

- ```{realm}/{clientId}/operations/assets/{assetId}/delete```
Deletes the specified asset.

##### [Attributes](https://github.com/openremote/openremote/blob/master/model/src/main/java/org/openremote/model/attribute/Attribute.java)
- ```{realm}/{clientId}/operations/assets/{assetId}/attributes/{attributeName}/update``` 
Updates the specified attribute of the specified asset.
- ```{realm}/{clientId}/operations/assets/{assetId}/attributes/update```
Updates the attributes of the specified asset based on the payload, allows for multi-attribute updating. Example: [multi attribute payload](#multi-attribute-update-payload).
- ```{realm}/{clientId}/operations/assets/{assetId}/attributes/get```
Request the attribute data of the specified asset.
- ```{realm}/{clientId}/operations/assets/{assetId}/attributes/{attributeName}/get```
Request the specified attribute data of the specified asset. The attribute data contains the full attribute object.
- ```{realm}/{clientId}/operations/assets/{assetId}/attributes/{attributeName}/get-value```
Request only the value of the specified attribute of the specified asset. The value is the raw value of the attribute.


### Events (Subscribing)
Events are subscription topics that allow for subscribing to various events such as new Assets 
being created or updates and attribute values being changed. 
Subscription events allow filtering through the usage of MQTT wildcard masks (+ and #).

### Notes:
- The response from the subscriptions are encoded in JSON.
- Using a **Gateway Asset V2 Service User** will enforce filters being relative to the associated Gateway Asset rather than the realm. This Service User can only receive event data from assets part of the Gateway Asset hierarchy.

##### [AssetEvent](https://github.com/openremote/openremote/blob/master/model/src/main/java/org/openremote/model/asset/AssetEvent.java)
- ```{realm}/{clientId}/events/assets/#```
All asset events of the realm.
- ```{realm}/{clientId}/events/assets/+```
All asset events for direct children of the realm.
- ```{realm}/{clientId}/events/assets/{assetId}```
All asset events for the specified asset.
- ```{realm}/{clientId}/events/assets/{assetId}/#```
All asset events for the descendants of the specified asset.
- ```{realm}/{clientId}/events/assets/{assetId}/+```
All asset events for the direct children of the specified asset.

##### [AttributeEvent](https://github.com/openremote/openremote/blob/master/model/src/main/java/org/openremote/model/attribute/AttributeEvent.java)
- ```{realm}/{clientId}/events/assets/+/attributes/#```
All attribute events of the realm.
- ```{realm}/{clientId}/events/assets/+/attributes/+```
All attribute events for direct children of the realm.
- ```{realm}/{clientId}/events/assets/+/attributes/{attributeName}/#```
All attribute events of the realm for the specified attribute name.
- ```{realm}/{clientId}/events/assets/+/attributes/{attributeName}/+```
All attribute events for direct children of the realm with the specified attribute name.
- ```{realm}/{clientId}/events/assets/{assetId}/attributes/+```
All attribute events for the direct children of the specified asset.
- ```{realm}/{clientId}/events/assets/{assetId}/attributes/{attributeName}```
All attribute events for the specified asset with the specified attribute name.
- ```{realm}/{clientId}/events/assets/{assetId}/attributes/{attributeName}/#```
All attribute events for descendants of the specified asset with the specified attribute name.
- ```{realm}/{clientId}/events/assets/{assetId}/attributes/{attributeName}/+```
All attribute events for direct children of the specified asset with the specified attribute name.


***
### Examples & Extra Information

#### [Asset Templates](#asset-templates)
> - Asset Templates are JSON objects that define the structure of an asset.
> - Asset templates can be obtained through the [Swagger API](https://staging.demo.openremote.io/swagger) by retrieving the asset data of an existing asset.
> ##### Example of an Asset Template
>```json
>{
>  "type": "PresenceSensorAsset",
>  "name":"Hallway A Presence Sensor",
>  "location":"",
>  "attributes": {
>    "presence": 0,
>    "notes": ""
>  }
>}
>```
Exact Asset Templates can be retrieved from the Swagger API by retrieving the asset data of an existing asset. [Swagger API](https://staging.demo.openremote.io/swagger)
***

#### [Multi-Attribute Update Payload](#multi-attribute-update-payload)
> - The multi-attribute update payload is a JSON object that contains the attribute names and values to be updated.
> - The attribute names and values are key-value pairs.
> - The attribute names must match the attribute names of the asset.
> - The attribute values must match the data type of the attribute.
> ##### Example of a Multi-Attribute Update Payload
> ```json
> {
>  "presence": 1,
>  "notes": "Motion detected"
> }
> ```
