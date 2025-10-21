/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
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

package com.mycompany.app.rest;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;
import org.openspaces.core.transaction.manager.DistributedJiniTxManagerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
//import springfox.documentation.swagger2.annotations.EnableSwagger2;


@RestController

@ImportResource("META-INF/spring/pu.xml")
@Configuration
@EnableTransactionManagement
@PropertySource("classpath:application.properties")
//@EnableSwagger2
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class Main {


    @Value("${space.name}")
    private String spaceName;

    @Value("${space.group}")
    private String group;

    @Value("${space.manager}")
    private String locators;

    @Bean
    BillBuddyDal billBuddyDal() throws Exception {
        BillBuddyDal dal = new BillBuddyDal();
        // Manually inject gigaSpace field since @Resource is not processed on factory method beans
        try {
            java.lang.reflect.Field gigaSpaceField = BillBuddyDal.class.getDeclaredField("gigaSpace");
            gigaSpaceField.setAccessible(true);
            gigaSpaceField.set(dal, gigaSpace());

            // Manually call init since @PostConstruct is not invoked
            java.lang.reflect.Method init = BillBuddyDal.class.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(dal);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize BillBuddyDal", e);
        }
        return dal;
    }

    @Bean
    MerchantDal merchantDal() throws Exception {
        MerchantDal dal = new MerchantDal();
        // Manually inject gigaSpace field since @Resource is not processed on factory method beans
        try {
            java.lang.reflect.Field gigaSpaceField = MerchantDal.class.getDeclaredField("gigaSpace");
            gigaSpaceField.setAccessible(true);
            gigaSpaceField.set(dal, gigaSpace());

            // Manually call init since @PostConstruct is not invoked
            java.lang.reflect.Method init = MerchantDal.class.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(dal);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MerchantDal", e);
        }
        return dal;
    }

    @Bean
    UserDal userDal() throws Exception {
        UserDal dal = new UserDal();
        // Manually inject gigaSpace field since @Resource is not processed on factory method beans
        try {
            java.lang.reflect.Field gigaSpaceField = UserDal.class.getDeclaredField("gigaSpace");
            gigaSpaceField.setAccessible(true);
            gigaSpaceField.set(dal, gigaSpace());

            // Manually call init since @PostConstruct is not invoked
            java.lang.reflect.Method init = UserDal.class.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(dal);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize UserDal", e);
        }
        return dal;
    }

    @Bean
    DataController dataController() throws Exception {
        DataController controller = new DataController();
        // Manually inject fields since @Resource is not processed on factory method beans
        try {
            java.lang.reflect.Field billBuddyDalField = DataController.class.getDeclaredField("billBuddyDal");
            billBuddyDalField.setAccessible(true);
            billBuddyDalField.set(controller, billBuddyDal());

            java.lang.reflect.Field spaceField = DataController.class.getDeclaredField("space");
            spaceField.setAccessible(true);
            spaceField.set(controller, gigaSpace());

            // Manually call initialize since @PostConstruct is not invoked
            controller.initialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DataController", e);
        }
        return controller;
    }

    // for Space proxy
    @Bean
    PlatformTransactionManager transactionManager() throws Exception {
        return new DistributedJiniTxManagerConfigurer().transactionManager();
    }

    @Bean
    public GigaSpace gigaSpace() throws Exception {
        PlatformTransactionManager ptm = transactionManager();
        SpaceProxyConfigurer configurer = new SpaceProxyConfigurer(spaceName)
                .lookupLocators(locators).lookupGroups(group);

        return new GigaSpaceConfigurer(configurer).transactionManager(ptm).gigaSpace();
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.basePackage("com.gs")).build();
    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(Main.class);
        //app.setDefaultProperties(Collections
        //        .singletonMap("server.port", port));
        app.run(args);

    }

}
