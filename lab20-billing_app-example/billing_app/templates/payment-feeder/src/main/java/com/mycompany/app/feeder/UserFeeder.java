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

package com.mycompany.app.feeder;

import com.mycompany.app.model.AccountStatus;
import com.mycompany.app.model.Address;
import com.mycompany.app.model.CountryNames;
import com.mycompany.app.model.User;
import org.openspaces.core.GigaSpace;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User feeder class reads the user name list which is stored in users data member.
 * It then loops through the list, creates a User object and writes it to the space.
 * <p>
 */

//@Component
public class UserFeeder {

	private static final Logger logger = Logger.getLogger(UserFeeder.class.getName());

	@Resource
	private GigaSpace gigaSpace;

	public void setGigaSpace(GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}

	private static final List<String> users = new ArrayList<String>();
	static {
		users.add("James Johnson");
		users.add("Peter Gardiner");
		users.add("Andrei Satovsky");
		users.add("Petr Kirul");
		users.add("Gerard D'Artagnan'");
		users.add("Hans Freihof");
		users.add("Sami Filppula");
		users.add("Niklas Nilsson");
		users.add("Marian Varga");
		users.add("Sigur Briem");
		users.add("Bill Klein");
		users.add("David King");
		users.add("Magic Jordan");
		users.add("Hana Brill");
		users.add("Mustafa Cohen");
		users.add("Michel Peet");
		users.add("Samantha Gold");
		users.add("Snoop Cat");
		users.add("Marian Voght");
		users.add("Sugar Baby");
	};

	public UserFeeder() {
	}

	// This method creates users using list of user names from the
	// users data member and randomized information.
	// It then writes the merchant object into the space using a GigaSpace space proxy.
	@PostConstruct
	public void postConstruct() {

		logger.log(Level.INFO, "Starting User Feeder");
		logger.log(Level.INFO, "Method: loadData - loads all users into the space");

		Integer userAccountId = 1;

		// Read a list of user names and iterate over them one by one.

		for (String userName : UserFeeder.users) {

			// Check if user by the selected id already exists in the space.

			User foundUser = gigaSpace.readById(User.class, userAccountId);

			// Write a user to the space only if it does not exist already in the space

			if (foundUser == null) {

				// Create user

				User user = new User();
				user.setName(userName);
				user.setBalance(0.0);
				user.setCreditLimit(-10000.0);

				user.setStatus(AccountStatus.ACTIVE);
				user.setUserAccountId(userAccountId);

				// Create User Address

				Address tempAddress = new Address();
				tempAddress.setCountry(CountryNames.values()[new Random().nextInt(CountryNames.values().length)]);
				tempAddress.setCity("gsUniversity.com");
				tempAddress.setState("GIGASPACES");
				tempAddress.setStreet("Here and There");
				tempAddress.setZipCode(new Random().nextInt());

				user.setAddress(tempAddress);

				// Write user to the space
				gigaSpace.write(user);

				logger.log(Level.INFO, "Added User object with name: " + user.getName());
			}
			userAccountId++;
		}

		logger.log(Level.INFO, "Finished UserFeeder.");
	}

}
