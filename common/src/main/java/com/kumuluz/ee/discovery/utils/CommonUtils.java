/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package com.kumuluz.ee.discovery.utils;

import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.discovery.enums.ServiceType;
import com.vdurmont.semver4j.Requirement;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Common utils for service discovery.
 *
 * @author Urban Malc
 * @author Jan Meznaric
 * @since 1.0.0
 */
public class CommonUtils {

    private static int lastInstanceServedIndex;

    public static String determineVersion(DiscoveryUtil discoveryUtil, String serviceName, String version,
                                          String environment, ServiceType serviceType) {

        // check, if version has special characters (*, ^, ~)
        // if true, use get getServiceVersions to get appropriate version
        // return version

        Requirement versionRequirement;
        try {
            versionRequirement = Requirement.buildNPM(version);
        } catch (SemverException se) {
            return version;
        }

        if (!version.contains("*") && !version.contains("x")) {
            try {
                new Semver(version, Semver.SemverType.NPM);
                return version;
            } catch (SemverException ignored) {
            }
        }

        Optional<List<String>> versionsOpt = discoveryUtil.getServiceVersions(serviceName, environment, serviceType);

        if (versionsOpt.isPresent()) {
            List<String> versions = versionsOpt.get();
            List<Semver> versionsSemver = new LinkedList<>();

            for (String versionString : versions) {
                Semver listVersion;
                try {
                    listVersion = new Semver(versionString, Semver.SemverType.NPM);
                } catch (SemverException se) {
                    continue;
                }

                versionsSemver.add(listVersion);
            }

            Collections.sort(versionsSemver);

            for (int i = versionsSemver.size() - 1; i >= 0; i--) {
                if (versionsSemver.get(i).satisfies(versionRequirement)) {
                    return versionsSemver.get(i).getOriginalValue();
                }
            }
        }

        return version;
    }

    public static Optional<URL> pickServiceInstanceRoundRobin(List<URL> serviceInstances) {

        if (!serviceInstances.isEmpty()) {
            int index = 0;
            if (serviceInstances.size() >= lastInstanceServedIndex + 2) {
                index = lastInstanceServedIndex + 1;
            }
            lastInstanceServedIndex = index;

            return Optional.of(serviceInstances.get(index));
        } else {
            return Optional.empty();
        }
    }

    public static String getBaseUrl(ServiceType serviceType) {
        String baseUrl = null;
        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();
        if(serviceType == ServiceType.GRPC) {
            baseUrl = configurationUtil.get("kumuluzee.grpc.server.base-url").orElse(null);
        }
        if(baseUrl == null) {
            baseUrl = EeConfig.getInstance().getServer().getBaseUrl();
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = configurationUtil.get("kumuluzee.base-url").orElse(null);
                if (baseUrl != null) {
                    try {
                        baseUrl = new URL(baseUrl).toString();
                    } catch (MalformedURLException e) {
                        baseUrl = null;
                    }
                }
            }
        }
        if(serviceType == ServiceType.GRAPHQL && baseUrl != null) {
            baseUrl += configurationUtil.get("kumuluzee.graphql.mapping").orElse("/graphql");
        }
        return baseUrl;
    }

    public static Integer getServicePort(ServiceType serviceType) {
        Integer servicePort = null;
        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();
        switch (serviceType) {
            case GRPC:
                servicePort = configurationUtil.getInteger("kumuluzee.grpc.server.http.port").orElse(null);
                if(servicePort == null) {
                    servicePort = configurationUtil.getInteger("kumuluzee.grpc.server.https.port").orElse(null);
                }
                if(servicePort != null) {
                    break;
                }
            default:
                servicePort = EeConfig.getInstance().getServer().getHttp().getPort();
                if (servicePort == null) {
                    servicePort = EeConfig.getInstance().getServer().getHttps().getPort();
                }
                if (servicePort == null) {
                    servicePort = configurationUtil.getInteger("port").orElse(8080);
                }
                break;
        }
        return servicePort;
    }
}
