/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openremote.container.web.jsapi;

import org.jboss.resteasy.util.PathHelper;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.zip.GZIPOutputStream;

/**
 * @author Stéphane Épardaud <stef@epardaud.fr>
 */
public class JSAPIWriter {

    private static final Logger LOG = Logger.getLogger(JSAPIWriter.class.getName());

    private static final long serialVersionUID = -1985015444704126795L;

    public void writeJavaScript(String base, HttpServletRequest req, HttpServletResponse resp,
                                Map<String, ServiceRegistry> serviceRegistries) throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Starting JS API client");
        }

        // RESTEASY-776
        // before writing generated javascript, we generate Etag and compare it with client request.
        // If nothing changed, we send back 304 Not Modified for client browser to use cached js.
        String ifNoneMatch = req.getHeader("If-None-Match");
        String etag = generateEtag(serviceRegistries);
        resp.setHeader("Etag", etag);

        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        for (Map.Entry<String, ServiceRegistry> entry : serviceRegistries.entrySet()) {
            String uri = base;
            if (entry.getKey() != null) uri += entry.getKey();

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(new BufferedWriter(stringWriter));
            writeJavaScript(uri, writer, entry.getValue());
            writer.flush();
            writer.close();

            if (clientIsGzipSupported(req)) {
                ByteArrayOutputStream compressedContent = new ByteArrayOutputStream();
                GZIPOutputStream gzipstream = new GZIPOutputStream(compressedContent);
                gzipstream.write(stringWriter.toString().getBytes());
                gzipstream.finish();

                // get the compressed content
                byte[] compressedBytes = compressedContent.toByteArray();

                // set appropriate HTTP headers
                resp.setContentLength(compressedBytes.length);
                resp.addHeader("Content-Encoding", "gzip");

                ServletOutputStream output = resp.getOutputStream();
                output.write(compressedBytes);
                output.flush();
                output.close();

            } else {
                ServletOutputStream output = resp.getOutputStream();
                byte[] bytes = stringWriter.toString().getBytes();
                resp.setContentLength(bytes.length);
                output.write(bytes);
                output.flush();
                output.close();
            }
        }

    }

    private boolean clientIsGzipSupported(HttpServletRequest req) {
        String encoding = req.getHeader("Accept-Encoding");
        return encoding != null && encoding.contains("gzip");
    }

    public void writeJavaScript(String uri, PrintWriter writer,
                                ServiceRegistry serviceRegistry) throws IOException {
        copyResource("client.js", writer);
        writer.println("REST.apiURL = '" + uri + "';");
        Set<String> declaredPrefixes = new HashSet<String>();
        printService(writer, serviceRegistry, declaredPrefixes);

    }


    private String generateEtag(Map<String, ServiceRegistry> serviceRegistries) {
        StringBuilder etagBuilder = new StringBuilder();
        for (Map.Entry<String, ServiceRegistry> entry : serviceRegistries.entrySet()) {
            if (entry.getKey() != null) etagBuilder.append(entry.getKey()).append(':');
            generateEtag(entry.getValue(), etagBuilder);
        }
        return String.valueOf(Math.abs(etagBuilder.toString().hashCode()));
    }

    private void generateEtag(ServiceRegistry serviceRegistry, StringBuilder etagBuilder) {
        for (MethodMetaData methodMetaData : serviceRegistry.getMethodMetaData()) {
            etagBuilder.append(methodMetaData.hashCode());

            for (ServiceRegistry subService : serviceRegistry.getLocators()) {
                generateEtag(subService, etagBuilder);
            }

        }
    }

    private void printService(PrintWriter writer, ServiceRegistry serviceRegistry, Set<String> declaredPrefixes) {


        for (MethodMetaData methodMetaData : serviceRegistry.getMethodMetaData()) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Generating JS API: " + methodMetaData.getKlass().getName() + "#" + methodMetaData.getUri());
            }
            String declaringPrefix = methodMetaData.getFunctionPrefix(); // TODO Add prefix path segment
            declarePrefix(writer, declaringPrefix, declaredPrefixes);

            for (String httpMethod : methodMetaData.getHttpMethods()) {
                print(writer, httpMethod, methodMetaData);
            }
        }
        for (ServiceRegistry subService : serviceRegistry.getLocators())
            printService(writer, subService, declaredPrefixes);
    }

    private void declarePrefix(PrintWriter writer, String declaringPrefix, Set<String> declaredPrefixes) {
        if (declaredPrefixes.add(declaringPrefix)) {
            int lastDot = declaringPrefix.lastIndexOf(".");
            if (lastDot == -1)
                writer.println("var " + declaringPrefix + " = {};");
            else {
                declarePrefix(writer, declaringPrefix.substring(0, lastDot), declaredPrefixes);
                writer.println(declaringPrefix + " = {};");
            }
        }

    }

    private void copyResource(String name, PrintWriter writer)
        throws IOException {
        Reader reader = new InputStreamReader(getClass().getResourceAsStream(name));
        char[] array = new char[1024];
        int read;
        while ((read = reader.read(array)) >= 0) {
            writer.write(array, 0, read);
        }
        reader.close();
    }

    private void print(PrintWriter writer, String httpMethod, MethodMetaData methodMetaData) {
        String uri = methodMetaData.getUri();
        writer.println("// " + httpMethod + " " + uri);
        writer.println(methodMetaData.getFunctionName() + " = function(_params");
        printURIParamsSignature(uri, writer);
        printEntityParamsSignature(methodMetaData, writer);
        writer.println("){");
        writer.println(" var params = _params ? _params : {};");
        writer.println(" var request = new REST.Request();");
        writer.println(" request.setMethod('" + httpMethod + "');");
        writer
            .println(" var uri = params.$apiURL ? params.$apiURL : REST.apiURL;");
        if (uri.contains("{")) {
            printURIParamCalls(uri, writer);
        } else {
            writer.println(" uri += '" + uri + "';");
        }
        printOtherParams(methodMetaData, writer);
        writer.println(" request.setURI(uri);");
        writer.println(" if(params.$username && params.$password)");
        writer
            .println("  request.setCredentials(params.$username, params.$password);");
        writer.println(" if(params.$accepts)");
        writer.println("  request.setAccepts(params.$accepts);");
        if (methodMetaData.getWants() != null) {
            writer.println(" else");
            writer.println("  request.setAccepts('" + methodMetaData.getWants()
                + "');");
        }

        writer.println("if (REST.antiBrowserCache == true) {");
        writer.println("  request.addQueryParameter('resteasy_jsapi_anti_cache', (new Date().getTime()));");
        writer.println("    var cached_obj = REST._get_cache_signature(REST._generate_cache_signature(uri));");

        writer.println("    if (cached_obj != null) { request.addHeader('If-Modified-Since', cached_obj[1]['Last-Modified']); request.addHeader('If-None-Match', cached_obj[1]['Etag']);}");

        writer.println("}");


        writer.println(" if(params.$contentType)");
        writer.println("  request.setContentType(params.$contentType);");
        writer.println(" else");
        writer.println("  request.setContentType('"
            + methodMetaData.getConsumesMIMEType() + "');");
        writer.println(" if(params.$callback){");
        writer.println("  request.execute(params.$callback);");
        writer.println(" }else{");
        writer.println("  var returnValue;");
        writer.println("  request.setAsync(false);");
        writer
            .println("  var callback = function(httpCode, xmlHttpRequest, value){ returnValue = value;};");
        writer.println("  request.execute(callback);");
        writer.println("  return returnValue;");
        writer.println(" }");
        writer.println("};");
    }

    private void printEntityParamsSignature(MethodMetaData methodMetaData, PrintWriter writer) {
        List<MethodParamMetaData> params = methodMetaData.getParameters();
        for (MethodParamMetaData methodParamMetaData : params) {
            if (methodParamMetaData.getParamType() == MethodParamMetaData.MethodParamType.ENTITY_PARAMETER) {
                writer.println(", " + methodParamMetaData.getParamName());
            }
        }
    }

    private void printOtherParams(MethodMetaData methodMetaData,
                                  PrintWriter writer) {
        List<MethodParamMetaData> params = methodMetaData.getParameters();
        for (MethodParamMetaData methodParamMetaData : params) {
            printParameter(methodParamMetaData, writer);
        }
    }

    private void printParameter(MethodParamMetaData metaData,
                                PrintWriter writer) {
        switch (metaData.getParamType()) {
            case QUERY_PARAMETER:
                print(metaData, writer, "QueryParameter");
                break;
            case HEADER_PARAMETER:
                print(metaData, writer, "Header");
                // FIXME: warn about forbidden headers:
                // http://www.w3.org/TR/XMLHttpRequest/#the-setrequestheader-method
                break;
            case COOKIE_PARAMETER:
                print(metaData, writer, "Cookie");
                break;
            case MATRIX_PARAMETER:
                print(metaData, writer, "MatrixParameter");
                break;
            case FORM_PARAMETER:
                print(metaData, writer, "FormParameter");
                break;
            case FORM:
                print(metaData, writer, "Form");
                break;
            case ENTITY_PARAMETER:
                // the entity
                writer.println(" if(" + metaData.getParamName() + ") {");
                writer.println("   if(params.$entityWriter) {");
                writer.println("      request.setEntity(params.$entityWriter.write("+metaData.getParamName()+"));");
                writer.println("   } else {");
                writer.println("      request.setEntity(" + metaData.getParamName() + ");");
                writer.println("   }");
                writer.println(" }");
                writer.println(" if(params.$entity) {");
                writer.println("   if(params.$entityWriter) {");
                writer.println("      request.setEntity(params.$entityWriter.write(params.$entity));");
                writer.println("   } else {");
                writer.println("      request.setEntity(params.$entity);");
                writer.println("   }");
                writer.println(" }");
                break;
        }
    }

    private void print(MethodParamMetaData metaData, PrintWriter writer, String type) {
        String paramName = metaData.getParamName();
        writer.println(String.format(" if(Object.prototype.hasOwnProperty.call(params, '%s'))\n  request.add%s('%s', params.%s);", paramName, type, paramName, paramName));
    }

    private void printURIParamsSignature(String uri, PrintWriter writer) {
        String replacedCurlyURI = PathHelper.replaceEnclosedCurlyBraces(uri);
        Matcher matcher = PathHelper.URI_PARAM_PATTERN.matcher(replacedCurlyURI);
        while (matcher.find()) {
            String name = matcher.group(1);
            writer.println(", " + name);
        }
    }

    private void printURIParamCalls(String uri, PrintWriter writer) {
        String replacedCurlyURI = PathHelper.replaceEnclosedCurlyBraces(uri);
        Matcher matcher = PathHelper.URI_PARAM_PATTERN.matcher(replacedCurlyURI);
        int i = 0;
        while (matcher.find()) {
            if (matcher.start() > i) {
                writer.println(" uri += '" + replacedCurlyURI.substring(i, matcher.start()) + "';");
            }
            String name = matcher.group(1);
            writer.println(" uri += REST.Encoding.encodePathSegment(" + name + ");");
            i = matcher.end();
        }
        if (i < replacedCurlyURI.length())
            writer.println(" uri += '" + replacedCurlyURI.substring(i) + "';");
    }

}
