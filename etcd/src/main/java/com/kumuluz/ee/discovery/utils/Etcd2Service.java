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

import com.kumuluz.ee.discovery.enums.ServiceType;

import java.net.URL;

/**
 * Runnable for service registration and heartbeats.
 *
 * @author Urban Malc
 * @author Jan Meznaric
 * @since 1.0.0
 */
public class Etcd2Service {

    private URL baseUrl;
    private URL containerUrl;
    private String clusterId;
    private ServiceType serviceType;

    public Etcd2Service(URL baseUrl, URL containerUrl, String clusterId) {
        this.baseUrl = baseUrl;
        this.containerUrl = containerUrl;
        this.clusterId = clusterId;
        //this.serviceType = serviceType;
    }

    public URL getBaseUrl() {
        return baseUrl;
    }

    public URL getContainerUrl() {
        if (containerUrl != null) {
            return this.containerUrl;
        } else {
            return this.baseUrl;
        }
    }

    public String getClusterId() {
        return this.clusterId;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setBaseUrl(URL baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setContainerUrl(URL containerUrl) {
        this.containerUrl = containerUrl;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }
}
