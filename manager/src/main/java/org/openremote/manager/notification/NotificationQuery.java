package org.openremote.manager.notification;

import java.util.List;
import java.util.ArrayList;

public class NotificationQuery {
    private List<Long> ids;
    private List<String> types;
    private Long fromTimestamp; 
    private Long toTimestamp;
    private List<String> realmIds;
    private List<String> userIds;
    private List<String> assetIds;

    public static class Builder {
        private NotificationQuery query = new NotificationQuery();

        public Builder id(Long id) {
            if (id != null) {
                if (query.ids == null) {
                    query.ids = new ArrayList<>();
                }

                query.ids.add(id);
            }
            return this; 
        }

        public Builder type(String type) {
            if (type != null) {
                if (query.types == null) {
                    query.types = new ArrayList<>();
                }
                query.types.add(type);
            }
            return this; 
        }

        public Builder fromTimestamp (Long timestamp) {
            query.fromTimestamp = timestamp;
            return this;
        }

        public Builder toTimestamp (Long timestamp) {
            query.toTimestamp = timestamp;
            return this;
        }

        public Builder realmId(String realmId) {
            if (realmId != null) {
                if (query.realmIds == null) {
                    query.realmIds = new ArrayList<>();
                }
                query.realmIds.add(realmId);
            }
            return this; 
        }
        
        public Builder userId(String userId) {
            if (userId != null) {
                if (query.userIds == null) {
                    query.userIds = new ArrayList<>();
                }
                query.userIds.add(userId);
            }
            return this; 
        }

        public Builder assetId(String assetId) {
            if (assetId != null) {
                if (query.assetIds == null) {
                    query.assetIds = new ArrayList<>();
                }
                query.assetIds.add(assetId);
            }
            return this; 
        }

        public NotificationQuery build() {
            // possible exceptions
            if (query.fromTimestamp != null && query.toTimestamp != null) {
                throw new IllegalArgumentException("Timestamp cannot be null.");
            }
            if (query.fromTimestamp > query.toTimestamp) {
                throw new IllegalArgumentException("fromTimestamp must be before toTimestamp.");
            }
            return query;
        }
    }

    //getters
    public List<Long> getIds() {return ids;}
    public List<String> getTypes() {return types;}
    public Long getFromTimestamp() {return fromTimestamp;}
    public Long getToTimestamp() {return toTimestamp;}
    public List<String> getRealmIds() {return types;}
    public List<String> getUserIds() {return userIds;}
    public List<String> getAssetIds() {return assetIds;}
}
