<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:keycloak="urn:jboss:domain:keycloak-server:1.1">

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="//keycloak:staticMaxAge/text()">-1</xsl:template>
    <xsl:template match="//keycloak:cacheThemes/text()">false</xsl:template>
    <xsl:template match="//keycloak:cacheTemplates/text()">false</xsl:template>

<!--
        <staticMaxAge>2592000</staticMaxAge>
        <cacheThemes>true</cacheThemes>
        <cacheTemplates>true</cacheTemplates>
        <dir>${jboss.home.dir}/themes</dir>

.theme.staticMaxAge |= -1 |
.theme.cacheTemplates |=false |
.theme.cacheThemes |= false
-->

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>