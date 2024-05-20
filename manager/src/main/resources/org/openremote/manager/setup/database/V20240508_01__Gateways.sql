-- Add tunnelSupported attribute for GatewayAsset
SELECT a.id, ADD_ATTRIBUTE(a, 'tunnelingSupported', 'boolean', null, now(), null) from asset a where a.type = 'GatewayAsset';
