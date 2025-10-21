### blueprint instructions

A blueprint is a template for a maven project. Use the instructions below to generate a complete maven project.

1. Copy the `billing_app` directory to the `$GS_HOME/config/blueprints` directory. For example `cp -r billing_app /home/dixson/gigaspaces-smart-cache-enterprise/config/blueprints`.
2. Go to `$GS_HOME/config/blueprints/billing_app`
3. Update `values.yaml` with relevant parameters to be used for the project. 
4. Go to the `$GS_HOME/bin` directory.
5. Generate the blueprints, for example: `./gs.sh blueprint generate billing_app billing_app`. This will generate the project in a sub-directory below the bin directory, for example: `$GS_HOME/bin/billing_app`