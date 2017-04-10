<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:undertow="urn:jboss:domain:undertow:3.0">

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="//undertow:host">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
            <xsl:element name="filter-ref" namespace="urn:jboss:domain:undertow:3.0">
                <xsl:attribute name="name">request-dumper</xsl:attribute>
            </xsl:element>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="//undertow:filters">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
            <xsl:element name="filter" namespace="urn:jboss:domain:undertow:3.0">
                <xsl:attribute name="name">request-dumper</xsl:attribute>
                <xsl:attribute name="module">io.undertow.core</xsl:attribute>
                <xsl:attribute name="class-name">io.undertow.server.handlers.RequestDumpingHandler</xsl:attribute>
            </xsl:element>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>