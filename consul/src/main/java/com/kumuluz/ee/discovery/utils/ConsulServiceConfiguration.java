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

import com.kumuluz.ee.common.runtime.EeRuntime;
import com.kumuluz.ee.discovery.enums.ServiceType;

/**
 * Service configuration data.
 *
 * @author Urban Malc
 * @since 1.0.0
 */
public class ConsulServiceConfiguration {
    private String serviceName;
    private String environment;
    private String version;
    private String serviceId;
    private String serviceProtocol;
    private String address;
    private int servicePort;
    private long ttl;
    private boolean singleton;
    private ServiceType serviceType;

    private int startRetryDelay;
    private int maxRetryDelay;
    private int deregisterCriticalServiceAfter;

    public ConsulServiceConfiguration(String serviceName, String environment, String version, String serviceProtocol,
                                      String address, int servicePort, long ttl, boolean singleton, int startRetryDelay,
                                      int maxRetryDelay, int deregisterCriticalServiceAfter, String serviceId, ServiceType serviceType) {
        this.serviceName = serviceName;
        this.environment = environment;
        this.version = version;
        if (serviceId == null) {
            this.serviceId = serviceName + "-" + EeRuntime.getInstance().getInstanceId();
        } else {
            this.serviceId = serviceId;
        }
        this.serviceProtocol = serviceProtocol;
        this.address = address;

        this.servicePort = servicePort;
        this.ttl = ttl;
        this.singleton = singleton;
        this.serviceType = serviceType;

        this.startRetryDelay = startRetryDelay;
        this.maxRetryDelay = maxRetryDelay;
        this.deregisterCriticalServiceAfter = deregisterCriticalServiceAfter;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getVersion() {
        return version;
    }

    public String getServiceConsulKey() {
        return ConsulUtils.getConsulServiceKey(this.serviceName, this.environment);
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getServiceProtocol() {
        return serviceProtocol;
    }

    public String getAddress() {
        return address;
    }

    public int getServicePort() {
        return servicePort;
    }

    public long getTtl() {
        return ttl;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public int getStartRetryDelay() {
        return startRetryDelay;
    }

    public int getMaxRetryDelay() {
        return maxRetryDelay;
    }

    public int getDeregisterCriticalServiceAfter() {
        return deregisterCriticalServiceAfter;
    }
}
