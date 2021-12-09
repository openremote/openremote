/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.bluetooth.mesh.transport;

import java.util.UUID;

import static org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils.RESOLUTION_100_MS;
import static org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils.RESOLUTION_10_M;
import static org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils.RESOLUTION_10_S;
import static org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils.RESOLUTION_1_S;

public class PublicationSettings {

    private static final int DEFAULT_PUBLISH_TTL = 0x7F;
    private static final int DEFAULT_PUBLICATION_STEPS = 0;
    private static final int DEFAULT_PUBLICATION_RESOLUTION = 0b00;
    public static final int MIN_PUBLICATION_RETRANSMIT_COUNT = 0b000;
    public static final int MAX_PUBLICATION_RETRANSMIT_COUNT = 0b111;
    private static final int DEFAULT_PUBLICATION_RETRANSMIT_INTERVAL_STEPS = 0b00000;
    private static final int MAX_PUBLICATION_RETRANSMIT_INTERVAL_STEPS = 0b11111;

    private int publishAddress;
    private UUID labelUUID;
    private int appKeyIndex;
    private boolean credentialFlag;
    private int publishTtl = DEFAULT_PUBLISH_TTL;
    private int publicationSteps = DEFAULT_PUBLICATION_STEPS;
    private int publicationResolution = DEFAULT_PUBLICATION_RESOLUTION;
    private int publishRetransmitCount = MIN_PUBLICATION_RETRANSMIT_COUNT;
    private int publishRetransmitIntervalSteps = DEFAULT_PUBLICATION_RETRANSMIT_INTERVAL_STEPS;

    public PublicationSettings() {
    }

    /**
     * Constructs a PublicationSettings
     *
     * @param publishAddress                 Address to which the element must publish
     * @param appKeyIndex                    Index of the application key
     * @param credentialFlag                 Credentials flag define which credentials to be used, set true to use
     *                                       friendship credentials and false for master credentials. Currently supports only master credentials
     * @param publishRetransmitCount         Number of publication retransmits
     * @param publishRetransmitIntervalSteps Publish retransmit interval steps
     */
    public PublicationSettings(final int publishAddress,
                               final int appKeyIndex,
                               final boolean credentialFlag,
                               final int publishRetransmitCount,
                               final int publishRetransmitIntervalSteps) {
        this(publishAddress, appKeyIndex, credentialFlag,
            DEFAULT_PUBLISH_TTL,
            DEFAULT_PUBLICATION_STEPS,
            DEFAULT_PUBLICATION_RESOLUTION,
            MIN_PUBLICATION_RETRANSMIT_COUNT,
            DEFAULT_PUBLICATION_RETRANSMIT_INTERVAL_STEPS);
    }

    /**
     * Constructs a PublicationSettings
     *
     * @param publishAddress                 Address to which the element must publish
     * @param appKeyIndex                    Index of the application key
     * @param credentialFlag                 Credentials flag define which credentials to be used, set true to use
     *                                       friendship credentials and false for master credentials. Currently supports only master credentials
     * @param publishTtl                     Publication ttl
     * @param publicationSteps               Publication steps for the publication period
     * @param publicationResolution          Publication resolution of the publication period
     * @param publishRetransmitCount         Number of publication retransmits
     * @param publishRetransmitIntervalSteps Publish retransmit interval steps
     */
    PublicationSettings(final int publishAddress,
                        final int appKeyIndex,
                        final boolean credentialFlag,
                        final int publishTtl,
                        final int publicationSteps,
                        final int publicationResolution,
                        final int publishRetransmitCount,
                        final int publishRetransmitIntervalSteps) {
        this(publishAddress, null, appKeyIndex, credentialFlag, publishTtl,
            publicationSteps, publicationResolution, publishRetransmitCount, publishRetransmitIntervalSteps);
    }

    /**
     * Constructs a PublicationSettings
     *
     * @param publishAddress                 Address to which the element must publish
     * @param appKeyIndex                    Index of the application key
     * @param credentialFlag                 Credentials flag define which credentials to be used, set true to use
     *                                       friendship credentials and false for master credentials. Currently supports only master credentials
     * @param publishTtl                     Publication ttl
     * @param publicationSteps               Publication steps for the publication period
     * @param publicationResolution          Publication resolution of the publication period
     * @param publishRetransmitCount         Number of publication retransmits
     * @param publishRetransmitIntervalSteps Publish retransmit interval steps
     */
    PublicationSettings(final int publishAddress,
                        final UUID labelUUID,
                        final int appKeyIndex,
                        final boolean credentialFlag,
                        final int publishTtl,
                        final int publicationSteps,
                        final int publicationResolution,
                        final int publishRetransmitCount,
                        final int publishRetransmitIntervalSteps) {
        this.publishAddress = publishAddress;
        this.labelUUID = labelUUID;
        this.appKeyIndex = appKeyIndex;
        this.credentialFlag = credentialFlag;
        this.publishTtl = publishTtl;
        this.publicationSteps = publicationSteps;
        this.publicationResolution = publicationResolution;
        this.publishRetransmitCount = publishRetransmitCount;
        this.publishRetransmitIntervalSteps = publishRetransmitIntervalSteps;
    }

    /**
     * Returns the publish address, this is the address the model may publish messages when set
     *
     * @return publish address
     */
    public int getPublishAddress() {
        return publishAddress;
    }

    /**
     * Sets a publish address for this model
     *
     * @param publishAddress publish address
     */
    public void setPublishAddress(final int publishAddress) {
        this.publishAddress = publishAddress;
    }

    /**
     * Returns the label uuid for this model
     */
    public UUID getLabelUUID() {
        return labelUUID;
    }

    /**
     * Sets the label uuid for the publication settings of the model
     *
     * @param labelUUID 16-byte label uuid
     */
    void setLabelUUID(final UUID labelUUID) {
        this.labelUUID = labelUUID;
    }

    /**
     * Returns the app key index used for publishing by this model
     *
     * @return Global app key index
     */
    public int getAppKeyIndex() {
        return appKeyIndex;
    }

    /**
     * Set app key index to be used when publishing messages.
     *
     * @param appKeyIndex global application key index
     */
    public void setAppKeyIndex(final int appKeyIndex) {
        this.appKeyIndex = appKeyIndex;
    }

    public boolean getCredentialFlag() {
        return credentialFlag;
    }

    /**
     * Sets the credential flags true if friendship credentials is to be used or false if master credentials flags must be used.
     *
     * @param credentialFlag credential flag
     */
    void setCredentialFlag(final boolean credentialFlag) {
        this.credentialFlag = credentialFlag;
    }

    /**
     * Returns the ttl used for publication.
     *
     * @return publication ttl
     */
    public int getPublishTtl() {
        return publishTtl & 0xFF;
    }

    /**
     * Sets the ttl used for publication.
     */
    void setPublishTtl(final int publishTtl) {
        this.publishTtl = publishTtl;
    }

    /**
     * Returns the retransmit count used in publication
     *
     * @return publication retransmit count
     */
    public int getPublishRetransmitCount() {
        return publishRetransmitCount;
    }

    /**
     * Sets the retransmit count used in publication
     */
    void setPublishRetransmitCount(final int publishRetransmitCount) {
        this.publishRetransmitCount = publishRetransmitCount;
    }

    /**
     * Returns the retransmit interval steps used in publication
     *
     * @return publication retransmit interval steps
     */
    public int getPublishRetransmitIntervalSteps() {
        return publishRetransmitIntervalSteps;
    }

    /**
     * Sets the retransmit interval steps used in publication
     */
    void setPublishRetransmitIntervalSteps(final int publishRetransmitIntervalSteps) {
        this.publishRetransmitIntervalSteps = publishRetransmitIntervalSteps;
    }

    /**
     * Returns the publication steps used for publication
     *
     * @return publication steps
     */
    public int getPublicationSteps() {
        return publicationSteps;
    }

    /**
     * Sets the publication steps used for publication
     */
    void setPublicationSteps(final int publicationSteps) {
        this.publicationSteps = publicationSteps;
    }

    /**
     * Returns the resolution bit-field of publication steps. The resolution can be 100ms, 1 second, 10 seconds or 10 minutes
     *
     * @return resolution
     */
    public int getPublicationResolution() {
        return publicationResolution;
    }

    void setPublicationResolution(final int publicationResolution) {
        this.publicationResolution = publicationResolution;
    }

    /**
     * Encodes the publication period as an interval based on the resolution.
     */
    int serializePublicationResolution() {
        switch (publicationResolution) {
            default:
            case RESOLUTION_100_MS:
                return 100;
            case RESOLUTION_1_S:
                return 1000;
            case RESOLUTION_10_S:
                return 10 * 1000;
            case RESOLUTION_10_M:
                return 10 * 1000 * 60;
        }
    }

    /**
     * Decodes the publication period resolution.
     *
     * @param resolution publication period resolution
     */
    public static int deserializePublicationResolution(final int resolution) {
        switch (resolution) {
            default:
            case 100:
                return RESOLUTION_100_MS;
            case 1000:
                return RESOLUTION_1_S;
            case 10000:
                return RESOLUTION_10_S;
            case 600000:
                return RESOLUTION_10_M;
        }
    }

    /**
     * Returns the publish period in seconds
     */
    public int getPublishPeriod() {
        switch (publicationResolution) {
            default:
            case RESOLUTION_100_MS:
                return ((100 * publicationSteps) / 1000);
            case RESOLUTION_1_S:
                return publicationSteps;
            case RESOLUTION_10_S:
                return (10 * publicationSteps);
            case RESOLUTION_10_M:
                return (10 * publicationSteps) * 60;
        }
    }

    /**
     * Returns the publish period in seconds
     */
    public static int getPublishPeriod(final int publicationResolution, final int publicationSteps) {
        switch (publicationResolution) {
            default:
            case 0b00:
                return 100 * publicationSteps;
            case 0b01:
                return publicationSteps;
            case 0b10:
                return 10 * publicationSteps;
            case 0b11:
                return 10 * publicationSteps * 60;
        }
    }

    /**
     * Returns the retransmission interval in milliseconds
     */
    public int getRetransmissionInterval() {
        return (publishRetransmitIntervalSteps + 1) * 50;
    }

    /**
     * Returns the retransmit interval for a given number of retransmit interval steps in milliseconds
     *
     * @param intervalSteps Retransmit interval steps
     */
    public static int getRetransmissionInterval(final int intervalSteps) {
        if (intervalSteps >= DEFAULT_PUBLICATION_RETRANSMIT_INTERVAL_STEPS && intervalSteps <= MAX_PUBLICATION_RETRANSMIT_INTERVAL_STEPS)
            return (intervalSteps + 1) * 50;
        return 0;
    }

    /**
     * Returns the minimum retransmit interval supported in milliseconds
     */
    public static int getMinRetransmissionInterval() {
        return (DEFAULT_PUBLICATION_RETRANSMIT_INTERVAL_STEPS + 1) * 50;
    }

    /**
     * Returns the maximum retransmit interval supported in milliseconds
     */
    public static int getMaxRetransmissionInterval() {
        return (MAX_PUBLICATION_RETRANSMIT_INTERVAL_STEPS + 1) * 50;
    }

    /**
     * Returns the retransmit interval steps from the retransmit interval
     *
     * @param retransmitInterval Retransmit interval in milliseconds
     */
    public static int parseRetransmitIntervalSteps(final int retransmitInterval) {
        if (retransmitInterval >= 0 && retransmitInterval <= getMaxRetransmissionInterval()) {
            return ((retransmitInterval / 50) - 1);
        }
        throw new IllegalArgumentException("Invalid retransmit interval");
    }
}
