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
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@RestController

@ImportResource("META-INF/spring/pu.xml")
@Configuration
@EnableTransactionManagement
@PropertySource("classpath:application.properties")
@EnableSwagger2
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class Main {


    @Value("${space.name}")
    private String spaceName;

    @Value("${space.group}")
    private String group;

    @Value("${space.manager}")
    private String locators;

    @Bean
    BillBuddyDal billBuddyDal() {
        return new BillBuddyDal();
    }

    @Bean
    MerchantDal merchantDal() {
        return new MerchantDal();
    }

    @Bean
    UserDal userDal() {
        return new UserDal();
    }

    @Bean
    DataController dataController() { return new DataController(); }

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