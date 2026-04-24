-- add a nullable realm column to the notification table
ALTER table NOTIFICATION 
    ADD COLUMN realm VARCHAR(255);