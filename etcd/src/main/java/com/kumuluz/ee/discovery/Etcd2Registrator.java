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
package com.kumuluz.ee.discovery;

import com.kumuluz.ee.discovery.exceptions.EtcdNotAvailableException;
import com.kumuluz.ee.discovery.utils.Etcd2ServiceConfiguration;
import com.kumuluz.ee.discovery.utils.Etcd2Utils;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Runnable for service registration and heartbeats.
 *
 * @author Urban Malc
 * @author Jan Meznaric
 * @since 1.0.0
 */
public class Etcd2Registrator implements Runnable {
    private static final Logger log = Logger.getLogger(Etcd2Registrator.class.getName());

    private EtcdClient etcd;
    private Etcd2ServiceConfiguration serviceConfig;
    private boolean resilience;

    private boolean isRegistered;

    public Etcd2Registrator(EtcdClient etcd, Etcd2ServiceConfiguration serviceConfig, boolean resilience) {
        this.etcd = etcd;
        this.serviceConfig = serviceConfig;
        this.resilience = resilience;
    }

    public void run() {
        if (!this.isRegistered) {
            this.registerToEtcd();
        } else {

            log.fine("Sending heartbeat. " + this.serviceConfig.getServiceInstanceKey());

            try {
                this.etcd.putDir(this.serviceConfig.getServiceInstanceKey()).prevExist(true)
                        .refresh(this.serviceConfig.getTtl()).send().get();
            } catch (SocketException | TimeoutException e) {
                handleTimeoutException(e);
            } catch (IOException e) {
                log.info("IO Exception. Cannot put given key: " + e);
            } catch (EtcdAuthenticationException e) {
                log.severe("Etcd authentication exception. Cannot put given key: " + e);
            } catch (EtcdException e) {
                if (e.isErrorCode(100)) {
                    log.warning("Etcd key not present: " + this.serviceConfig.getServiceInstanceKey() +
                            ". Reregistering service.");

                    this.isRegistered = false;
                    this.registerToEtcd();
                } else {
                    e.printStackTrace();
                }
            }
        }
    }

    private void registerToEtcd() {
        if (this.serviceConfig.isSingleton() && isRegistered()) {

            log.warning("Instance was not registered. Trying to register a singleton microservice instance, but " +
                    "another instance is already registered.");

        } else {

            if (this.etcd != null) {
                log.info("Registering service with etcd. Service ID: " + this.serviceConfig.getServiceKeyUrl());

                try {
                    etcd.putDir(this.serviceConfig.getServiceInstanceKey()).ttl(this.serviceConfig.getTtl())
                            .send().get();
                    etcd.put(this.serviceConfig.getServiceInstanceKey() + "/type",
                            this.serviceConfig.getServiceType().toString()).send().get();
                    etcd.put(this.serviceConfig.getServiceKeyUrl(), this.serviceConfig.getBaseUrl()).send().get();
                    if (this.serviceConfig.getContainerUrl() != null) {
                        etcd.put(this.serviceConfig.getServiceInstanceKey() + "/containerUrl",
                                this.serviceConfig.getContainerUrl()).send().get();
                    }
                    if (this.serviceConfig.getClusterId() != null) {
                        etcd.put(this.serviceConfig.getServiceInstanceKey() + "/clusterId",
                                this.serviceConfig.getClusterId()).send().get();
                    }
                    this.isRegistered = true;
                } catch (SocketException | TimeoutException e) {
                    handleTimeoutException(e);
                } catch (IOException e) {
                    log.info("IO Exception. Cannot put given key: " + e);
                } catch (EtcdException e) {
                    log.info("Etcd exception. " + e);
                } catch (EtcdAuthenticationException e) {
                    log.severe("Etcd authentication exception. Cannot put given key: " + e);
                }
            } else {
                log.severe("etcd not initialised.");
            }

        }
    }

    private boolean isRegistered() {
        String serviceInstancesKey = Etcd2Utils.getServiceKeyInstances(this.serviceConfig.getEnvironment(),
                this.serviceConfig.getServiceName(), this.serviceConfig.getServiceVersion());

        EtcdKeysResponse etcdKeysResponse = Etcd2Utils.getEtcdDir(this.etcd, serviceInstancesKey, this.resilience);

        if (etcdKeysResponse != null) {
            for (EtcdKeysResponse.EtcdNode node : etcdKeysResponse.getNode().getNodes()) {

                String url = null;
                boolean isActive = true;
                for (EtcdKeysResponse.EtcdNode instanceNode : node.getNodes()) {

                    if ("url".equals(Etcd2Utils.getLastKeyLayer(instanceNode.getKey())) &&
                            instanceNode.getValue() != null) {
                        url = instanceNode.getValue();
                    }

                    if ("status".equals(Etcd2Utils.getLastKeyLayer(instanceNode.getKey())) &&
                            "disabled".equals(instanceNode.getValue())) {
                        isActive = false;
                    }

                }
                if (isActive && url != null) {
                    return true;
                }
            }
        }

        return false;
    }

    private void handleTimeoutException(Throwable e) {
        String message = "Timeout exception. Cannot read given key in specified time or retry-count " +
                "constraints.";
        if (resilience) {
            log.warning(message + " Error: " + e);
        } else {
            RuntimeException ex = new EtcdNotAvailableException(message, e);
            // print stack trace, because Exceptions in scheduler are not reported
            ex.printStackTrace();
            throw ex; // stops the scheduler
        }
    }
}
