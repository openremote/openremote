package org.openremote.manager.server.notification;


public class FCMMessage extends FCMBaseMessage {

        private Notification notification;
        private boolean content_available;
        private String priority;


        public FCMMessage(Notification notification, boolean content_available, String priority, String to) {
            super(to);
            this.notification = notification;
            this.content_available = content_available;
            this.priority = priority;
        }

        public Notification getNotification() {
            return notification;
        }

        public void setNotification(Notification notification) {
            this.notification = notification;
        }

        public boolean getContent_available() {
            return content_available;
        }

        public void setContent_available(boolean content_available) {
            this.content_available = content_available;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }


}
