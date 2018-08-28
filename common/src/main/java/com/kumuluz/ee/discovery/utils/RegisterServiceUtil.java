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
import com.kumuluz.ee.common.runtime.EeRuntime;
import com.kumuluz.ee.common.runtime.EeRuntimeExtension;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.discovery.annotations.RegisterService;
import com.kumuluz.ee.discovery.enums.ServiceType;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.core.Application;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Class for initialising service registration based on RegisterService annotation.
 *
 * @author Jan Meznaric
 * @since 1.0.0
 */
@ApplicationScoped
public class RegisterServiceUtil {

    private static final Logger log = Logger.getLogger(RegisterServiceUtil.class.getName());

    @Inject
    private DiscoveryUtil discoveryUtil;

    public void cdiInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {

        initialiseBean();

    }

    private void initialiseBean() {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream is = classLoader.getResourceAsStream("META-INF/kumuluzee/discovery/com.kumuluz.ee.discovery.RegisterService");
        if (is != null) {
            Scanner scanner = new Scanner(is);
            boolean registered = false;
            Class <?> grpcAnnotation = null;
            Class <?> graphqlAnnotation = null;
            for(EeRuntimeExtension eeRuntimeExtension : EeRuntime.getInstance().getEeExtensions()) {
                if (eeRuntimeExtension.getGroup().equalsIgnoreCase("grpc")) {
                    try {
                        grpcAnnotation = Class.forName("com.kumuluz.ee.grpc.annotations.GrpcService");
                    } catch (ClassNotFoundException e) {
                        log.warning("Couldn't load needed annotation for gRPC extension.");
                    }
                } else if(eeRuntimeExtension.getGroup().equalsIgnoreCase("graphql")) {
                    try {
                        graphqlAnnotation = Class.forName("com.kumuluz.ee.graphql.annotations.GraphQLApplicationClass");
                    } catch (ClassNotFoundException e) {
                        log.warning("Couldn't load needed annotation for GraphQL extension.");
                    }
                }
            }
            while (scanner.hasNextLine()) {
                if(registered) {
                    log.warning("Service already registered, skipping further registrations. KumuluzEE Discovery allows registration of one service per microservice.");
                    break;
                }
                String service = scanner.nextLine();
                try {
                    Class klass = Class.forName(service);
                    if (Application.class.isAssignableFrom(klass)) {
                        registerService(klass, ServiceType.REST);
                        registered = true;
                    } else if (grpcAnnotation != null && klass.isAnnotationPresent(grpcAnnotation)) {
                        registerService(Class.forName(service), ServiceType.GRPC);
                        registered = true;
                    } else if (graphqlAnnotation != null && klass.isAnnotationPresent(graphqlAnnotation)) {
                        registerService(Class.forName(service), ServiceType.GRAPHQL);
                        registered = true;
                    } else {
                        log.warning("Missing dependencies for GraphQL/gRPC. Service was not registered.");
                    }
                } catch (ClassNotFoundException e) {
                    log.warning(e.getMessage());
                }
            }
            scanner.close();
        }
    }

    /**
     * Method initialises class fields from configuration.
     */
    private void registerService(Class targetClass, ServiceType serviceType) {

        if (targetClassIsProxied(targetClass)) {
            targetClass = targetClass.getSuperclass();
        }

        RegisterService registerServiceAnnotation = (RegisterService) targetClass.getAnnotation(RegisterService.class);

        if (registerServiceAnnotation != null) {

            EeConfig eeConfig = EeConfig.getInstance();
            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

            String serviceName = eeConfig.getName();
            if (serviceName == null || serviceName.isEmpty()) {
                serviceName = configurationUtil.get("kumuluzee.service-name").orElse(null);
                if (serviceName == null || serviceName.isEmpty()) {
                    serviceName = registerServiceAnnotation.value();

                    if (serviceName.isEmpty()) {
                        serviceName = targetClass.getName();
                    }
                }
            }

            long ttl = configurationUtil.getInteger("kumuluzee.discovery.ttl").orElse(-1);
            if (ttl == -1) {
                ttl = registerServiceAnnotation.ttl();

                if (ttl == -1) {
                    ttl = 30;
                }
            }

            long pingInterval = configurationUtil.getInteger("kumuluzee.discovery.ping-interval").orElse(-1);
            if (pingInterval == -1) {
                pingInterval = registerServiceAnnotation.pingInterval();

                if (pingInterval == -1) {
                    pingInterval = 20;
                }
            }

            String environment = eeConfig.getEnv().getName();
            if (environment == null || environment.isEmpty()) {
                environment = configurationUtil.get("kumuluzee.env").orElse(null);
                if (environment == null || environment.isEmpty()) {
                    environment = registerServiceAnnotation.environment();

                    if (environment.isEmpty()) {
                        environment = "dev";
                    }
                }
            }

            String version = eeConfig.getVersion();
            if (version == null || version.isEmpty()) {
                version = configurationUtil.get("kumuluzee.version").orElse(null);
                if (version == null || version.isEmpty()) {
                    version = registerServiceAnnotation.version();

                    if (version.isEmpty()) {
                        version = "1.0.0";
                    }
                }
            }

            boolean singleton = registerServiceAnnotation.singleton();

            log.info("Registering " + serviceType.toString() +  " service: " + serviceName);

            discoveryUtil.register(serviceName, version, environment, ttl, pingInterval, singleton, serviceType);

        }
    }

    /**
     * Check if target class is proxied.
     *
     * @param targetClass target class
     * @return true if target class is proxied
     */
    private boolean targetClassIsProxied(Class targetClass) {
        return targetClass.getCanonicalName().contains("$Proxy");
    }


    @PreDestroy
    public void deregisterService() {
        discoveryUtil.deregister();
    }

}
