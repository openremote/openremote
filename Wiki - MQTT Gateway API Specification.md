# MQTT Gateway API
The MQTT Gateway API is a publish subscribe API providing a full suite of asset management controls and event subscriptions. 

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

***
## MQTT API Specification

### Operations (Publish)
Operations are publish topics that provide Asset management functionality, 
each operation topic has an associated *response* topic that can be subscribed to which will receive a success or error response.

#### Notes:
- Each operations topic has corresponding response topic suffix, this can be subscribed to beforehand. 
Example:```{realm}/{clientId}/operations/assets/{responseIdentifier}/create/response```
- Payloads will always use JSON encoding.


##### [Assets]()
Topics:

- ```{realm}/{clientId}/operations/assets/{responseIdentifier}/create``` 
Create an asset. Requires a valid [asset template]() as the payload.
<br />
- ```{realm}/{clientId}/operations/assets/{assetId}/get```
Request the data of the specified assetId. 
<br />
- ```{realm}/{clientId}/operations/assets/{assetId}/update```
Updates the specified asset. Requires a valid [update template]() as the payload.
<br />
- ```{realm}/{clientId}/operations/assets/{assetId}/delete```
Deletes the specified asset.



##### [Attributes]()
Topics:

- ```{realm}/{clientId}/operations/assets/{assetId}/attributes/{attributeName}/update``` 
Updates the specified attribute of the specified asset. 
<br />
- ```{realm}/{clientId}/operations/assets/{assetId}/attributes/update```
Updates the attributes of the specified asset based on the payload, allows for multi-attribute updating.
<br />
- ```{realm}/{clientId}/operations /assets/{assetId}/attributes/get```
Request the attribute data of the specified asset.
<br />
- ```{realm}/{clientId}/operations /assets/{assetId}/attributes/{attributeName}/get```
Request the specified attribute value of the specified asset.








### Events (Subscribing)
Events are subscription topics that allow for subscribing to various events such as new Assets 
being created or updates and attribute values being changed. 
Subscription events allow filtering through the usage of MQTT wildcard masks (+ and #).

##### [AssetEvent](https://github.com/openremote/openremote/blob/master/model/src/main/java/org/openremote/model/asset/AssetEvent.java)


##### [AttributeEvent](https://github.com/openremote/openremote/blob/master/model/src/main/java/org/openremote/model/attribute/AttributeEvent.java)
